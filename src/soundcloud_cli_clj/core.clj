(ns soundcloud-cli-clj.core
  (:require [soundcloud-cli-clj.cmd :as cmd]
            [soundcloud-cli-clj.player :as player]
            [soundcloud-cli-clj.config :as conf])
  (:gen-class))

(def curr-state (agent {}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (cmd/start! (do
                (send curr-state assoc :config (conf/load-config))
                (send curr-state assoc :player (player/create-player)))))
