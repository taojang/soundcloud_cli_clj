(ns soundcloud-cli-clj.player
  (:import [java.io Reader
                    Writer]
           [java.lang Process
                      ProcessBuilder])
  (:require [clojure.java.io :refer [reader writer]]
            [clojure.core.async :as async :refer [chan go go-loop sliding-buffer >! close!]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(declare create-player)

(defprotocol IPlayer
  (play-url [this url] "play url")
  (toggle-pause [this] "toggle resume/pause")
  (stop-playback [this] "stop underlying player process")
  (close [this] "end underlying player")
  (position-percent [this] "return the current position in percentage")
  (position-length [this] "return the current position in seconds")
  (length [this] "return the length of song in seconds"))

(defrecord Player [^Reader stdout ^Writer stdin ^Reader stderr ^Process process stdout-chan]
  IPlayer
  component/Lifecycle
  (start [this]
    (println "starting player...")
    (if (nil? (:process this))
      (create-player) ;; create new when there is no proecss
      this))          ;; avoiding create extra instance
  (stop [this]
    (println "shutting down player...")
    (if (nil? (:process this))
      this
      (do             ;; when not shutdown
        (close! stdout-chan)
        (.destroyForcibly process)
        (assoc this
               :stdout nil
               :stdin nil
               :stderr nil
               :process nil
               :stdout-chan nil))))
  (play-url [this url]
    (.write stdin (str "loadfile " "\"" url "\"" "\n"))
    (.flush stdin)
    this)
  (toggle-pause [this]
    (.write stdin "pause\n")
    (.flush stdin)
    this)
  (stop-playback [this]
    (.write stdin "stop\n")
    (.flush stdin)
    this)
  (close [this]
    (.destroy process)
    this)
  ; TODO: use core.async to implement following methods
  (position-percent [this] this)
  (position-length [this] this)
  (length [this] this))

(defn is-eof
  [line]
  (and (string? line)
       (.startsWith line "EOF code:")))

(defn create-player
  []
  (let [process (-> (ProcessBuilder. ["mplayer" "-really-quiet" "-slave" "-idle" "-msglevel" "global=6"])
                    (.start))]
    (let [mp-map {:stdout (-> process
                              (.getInputStream)
                              (reader))
                  :stdin (-> process
                             (.getOutputStream)
                             (writer))
                  :stderr (-> process
                              (.getErrorStream)
                              (reader))
                  :process process}
          stdout-chan (chan (sliding-buffer 1))]
      (go-loop [line (.readLine (:stdout mp-map))]
        (when line
          (>! stdout-chan line))
        (when (.startsWith line "EOF code: 1")
          (println "playback ends"))
        (recur (.readLine (:stdout mp-map))))
      (map->Player (assoc mp-map :stdout-chan stdout-chan)))))
