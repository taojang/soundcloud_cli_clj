(ns soundcloud-cli-clj.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [soundcloud-cli-clj.cmd :as cmd]
            [soundcloud-cli-clj.player :as player]
            [soundcloud-cli-clj.config :as conf])
  (:gen-class))

(def system
  (component/system-map
   :sys-chan (async/chan)
   :config (conf/load-config)
   :player (player/create-player)
   :cmd (component/using
         (cmd/new-cmd)
         [:player :config :sys-chan])))

(defn wait!
  []
  (let [s (java.util.concurrent.Semaphore. 0)]
    (.acquire s)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (component/stop-system system)))))
  (component/start-system system)
  (wait!))

