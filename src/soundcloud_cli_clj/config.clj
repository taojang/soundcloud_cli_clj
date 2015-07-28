(ns soundcloud-cli-clj.config
  (:require [clojure.edn :as edn]))

(def config-path
  (let [f-sep (System/getProperty "file.separator")
        home  (System/getProperty "user.home")]
    (clojure.string/join f-sep [home ".config" "sc_cli_clj" "config.edn"])))
