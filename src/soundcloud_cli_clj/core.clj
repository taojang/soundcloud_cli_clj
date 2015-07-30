(ns soundcloud-cli-clj.core
  (:require [soundcloud-cli-clj.cmd :as cmd])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (cmd/start!))
