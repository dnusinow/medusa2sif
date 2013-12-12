(defproject medusa2sif "0.5.0"
  :description "Convert Medusa network format to SIF"
  :url "http://example.com/FIXME"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.0-beta1"]]
  :profiles {:uberjar {:aot :all}}
  :main medusa2sif.core)
