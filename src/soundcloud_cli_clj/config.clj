(ns soundcloud-cli-clj.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :refer [make-parents]]
            [taoensso.timbre :as timbre
             :refer (log error info warn fatal)]))

(def config-path
  (let [f-sep (System/getProperty "file.separator")
        home  (System/getProperty "user.home")]
    (clojure.string/join f-sep [home ".config" "sc_cli_clj" "config.edn"])))

(def default-config
  {:client-id "client id"
   :client-secret "client secret"
   :oauth-token "oauth token"})

(defn load-config
  [& {:keys [path] :or {path config-path}}]
  (try
    (make-parents path)
    (-> path
        (slurp)
        (edn/read-string))
    (catch Exception ex
      (error (str "Error while loading config: " (.getMessage ex)))
      default-config)))

(defn save-config!
  [config & {:keys [path] :or {path config-path}}]
  (let [conf (select-keys
              (merge default-config config)
              (keys default-config))]
    (try
      (make-parents path)
      (spit path conf)
      true
      (catch Exception ex
        (error (str "Error while saving config: " (.getMessage ex)))
        false))))
