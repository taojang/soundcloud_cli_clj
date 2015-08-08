(ns soundcloud-cli-clj.cmd
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter])
  (:require [soundcloud-cli-clj.config :as config]
            [soundcloud-cli-clj.api :as api]
            [soundcloud-cli-clj.player :as player]
            [clojure.core.async :as async :refer [thread go-loop <! >! filter<]]
            [com.stuartsierra.component :as component]))


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
  [cmd]
  (let [cr         (ConsoleReader.)
        uname      (do
                     (.setPrompt cr "user name: ")
                     (.readLine cr))
        pass       (read-pass "password: ")
        orig-conf  (:config cmd)
        token      (api/get-token (:client-id orig-conf)
                                  (:client-secret orig-conf)
                                  uname
                                  pass)
        new-conf  (assoc orig-conf :oauth-token token)]
    (do
      ;; todo, somehow update config in system, maybe by sending something to sys-chan
      ;(send state assoc :config new-conf)
      (config/save-config! new-conf))))

(defn play-stream!
  [cmd]
  (let [conf     (:config cmd)
        ; TODO: shutdown chan
        eof-chan (filter< player/is-eof
                          (-> cmd
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
            (println init-track)
            (println (-> init-track
                         (:origin)
                         (:stream-url)
                         (api/create-stream-url (:client-id conf))))
            (go-loop [track init-track]
              (let [track-url (-> track
                                  (:origin)
                                  (:stream_url)
                                  (api/create-stream-url (:client-id conf)))]
                (println track)
                (player/play-url (:player cmd) track-url)
                (<! eof-chan)
                (recur (api/get-next-stream-track stream track)))))))
      (println "I don't do a lot..."))))

(defn start!
  [cmd]
  (let [cr            (ConsoleReader.)
        cmd-completer (StringsCompleter. (map name command-list))]
    (do
      (.addCompleter cr cmd-completer)
      (.setPrompt cr "sc-cmd=> "))
    ;; TODO: core.async
    (loop [l  (.readLine cr)]
      (condp = (cmd-from-str l)
        :login       (login! cmd)
        :play-stream (play-stream! cmd)
        :pause       (player/toggle-pause (:player cmd))
        :stop        (player/stop-playback (:player cmd))
        nil          (do (println "\nC-d pressed, bye for now")
                         (System/exit 0))
        (println "unkown command"))
      (recur (.readLine cr)))))

(defrecord Cmd [player config sys-chan]
  component/Lifecycle
  (start [this]
    (println "starting Cmd component...")
    (async/thread (start! this))
    this)
  (stop [this]
    (println "shutting down Cmd component...")
    this))

(defn new-cmd
  []
  (map->Cmd {}))
