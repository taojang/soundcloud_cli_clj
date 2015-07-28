(defproject soundcloud-cli-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/timbre "4.0.2"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [me.raynes/conch "0.8.0"]]
  :main ^:skip-aot soundcloud-cli-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
