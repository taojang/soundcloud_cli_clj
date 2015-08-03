(ns soundcloud-cli-clj.cmd
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter])
  (:require [soundcloud-cli-clj.config :as config]
            [soundcloud-cli-clj.api :as api]
            [soundcloud-cli-clj.player :as player]))

(def command-list
  #{:help :login :play-stream :play :next :prev :pause :stop :current})

(defn cmd-from-str
  [s]
  (when s
    (-> s
        (clojure.string/trimr)
        (keyword))))

(defn read-pass
  [promt-str]
  (let [cr (ConsoleReader.)]
    (.readLine cr promt-str \*)))

(defn login!
  []
  (let [cr         (ConsoleReader.)
        uname      (do
                     (.setPrompt cr "user name: ")
                     (.readLine cr))
        pass       (read-pass "password: ")
        orig-conf  (config/load-config)
        token      (api/get-token (:client-id orig-conf)
                                  (:client-secret orig-conf)
                                  uname
                                  pass)]
    (config/save-config! (assoc orig-conf :oauth-token token))))

(defn play-stream!
  [state]
  (let [conf (:config @state)]
    (do
      (println "loading stream...")
      ; TODO track current state, play next automatically
      (when-let [stream (-> (api/get-my-stream (:oauth-token conf))
                            (:collection)
                            (first))]
        (let [stream-url (-> stream
                             (:origin)
                             (:stream_url)
                             (api/create-stream-url (:client-id conf)))]
          (do
            (println stream-url)
            (player/play-url (:player @state) stream-url))))
      (println "I don't do a lot..."))))

(defn clean-up!
  [state]
  (do
    (println "Shutting down mplayer")
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
        :login       (login!)
        :play-stream (play-stream! state)
        :pause       (player/toggle-pause (:player @state))
        :stop        (player/stop (:player @state))
        nil          (do (println "\nC-d pressed, bye for now")
                         (System/exit 0))
        (println "unkown command"))
      (recur (.readLine cr)))))
