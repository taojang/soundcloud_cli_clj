(ns soundcloud-cli-clj.cmd
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter])
  (:require [soundcloud-cli-clj.config :as config]
            [soundcloud-cli-clj.api :as api]))

(def curr-state (atom {}))

(def command-list
  #{:help :login :play-stream :play :next :prev :pause :stop :current})

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
  []
  (let [conf (config/load-config)]
    (do
      (println "loading stream...")
      (println "I don't do a lot..."))))

(defn start!
  []
  (let [cr            (ConsoleReader.)
        cmd-completer (StringsCompleter. (map name command-list))]
    (do
      (.addCompleter cr cmd-completer)
      (.setPrompt cr "sc-cmd=> "))
    ;; TODO: core.async
    (loop [l  (.readLine cr)]
      (condp = (-> l (clojure.string/trimr) (keyword))
        :login (login!)
        :play-stream (play-stream!)
        nil (do (println "\nC-d pressed, bye for now")
                (System/exit 0))
        (println "unkown command"))
      (recur (.readLine cr)))))
