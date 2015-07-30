(ns soundcloud-cli-clj.cmd
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter]))

(def curr-state (atom {}))

(def command-list
  #{:help :login :play-stream :play :next :pause :stop :current})

(defn read-pass
  [promt-str]
  (let [cr (ConsoleReader.)]
    (.readLine cr promt-str \*)))

(defn start!
  []
  (let [cr            (ConsoleReader.)
        cmd-completer (StringsCompleter. (map name command-list))]
    (.addCompleter cr cmd-completer)
    (.setPrompt cr "sc-cmd==>")
    ;; TODO: core.async
    (loop [l (.readLine cr)]
      (println (keyword l))
      (recur (.readLine cr)))))
