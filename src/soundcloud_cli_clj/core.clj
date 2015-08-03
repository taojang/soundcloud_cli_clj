(ns soundcloud-cli-clj.core
  (:require [soundcloud-cli-clj.cmd :as cmd]
            [soundcloud-cli-clj.player :as player])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (cmd/start! (player/create-player)))
