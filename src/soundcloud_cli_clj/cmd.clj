(ns soundcloud-cli-clj.cmd
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter])
  (:require [soundcloud-cli-clj.config :as config]
            [soundcloud-cli-clj.api :as api]
            [soundcloud-cli-clj.player :as player]
            [clojure.core.async :as async :refer [go-loop <! >! filter<]]))

(def command-list
  #{:help :login :play-stream :play :next :prev :pause :stop :current})

(defn cmd-from-str
  [s]
  ; TODO: handle empty string
  (when s
    (-> s
        (clojure.string/trimr)
        (keyword))))

(defn read-pass
  [promt-str]
  (let [cr (ConsoleReader.)]
    (.readLine cr promt-str \*)))

(defn login!
  [state]
  (let [cr         (ConsoleReader.)
        uname      (do
                     (.setPrompt cr "user name: ")
                     (.readLine cr))
        pass       (read-pass "password: ")
        orig-conf  (:config @state)
        token      (api/get-token (:client-id orig-conf)
                                  (:client-secret orig-conf)
                                  uname
                                  pass)
        new-conf  (assoc orig-conf :oauth-token token)]
    (do
      (send state assoc :config new-conf)
      (config/save-config! new-conf))))

(defn play-stream!
  [state]
  (let [conf     (:config @state)
        ; TODO: shutdown chan
        eof-chan (filter< player/is-eof
                          (-> @state
                              (:player)
                              (:stdout-chan)))]
    (do
      (println "loading stream...")
      ; TODO track current state, play next automatically
      (when-let [stream (api/get-my-stream (:oauth-token conf))]
        (let [init-track  (-> stream
                              (:collection)
                              ((fn [coll]
                                 (sort-by (comp :duration :origin) coll)))
                              (first))]
          (do
            (send state assoc :stream stream)
            (println init-track)
            (go-loop [track init-track]
              (let [track-url (-> track
                                  (:origin)
                                  (:stream_url)
                                  (api/create-stream-url (:client-id conf)))]
                (println track)
                (player/play-url (:player @state) track-url)
                (<! eof-chan)
                (recur (api/get-next-stream-track (:stream @state) track)))))))
      (println "I don't do a lot..."))))

(defn clean-up!
  [state]
  (do
    (println "Shutting down mplayer")
    (-> @state
        (:player)
        (:stdout-chan)
        (async/close!))
    (-> @state
        (:player)
        (:process)
        (.destroy))))

(defn start!
  [state]
  (let [cr            (ConsoleReader.)
        cmd-completer (StringsCompleter. (map name command-list))]
    (do
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn [] (clean-up! state))))
      (.addCompleter cr cmd-completer)
      (.setPrompt cr "sc-cmd=> "))
    ;; TODO: core.async
    (loop [l  (.readLine cr)]
      (condp = (cmd-from-str l)
        :login       (login! state)
        :play-stream (play-stream! state)
        :pause       (player/toggle-pause (:player @state))
        :stop        (player/stop (:player @state))
        nil          (do (println "\nC-d pressed, bye for now")
                         (System/exit 0))
        (println "unkown command"))
      (recur (.readLine cr)))))
