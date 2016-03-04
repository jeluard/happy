(defproject happy "0.5.3-SNAPSHOT"
  :description "Clojure(Script) HTTP async client library"
  :url "http://github.com/jeluard/happy"
  :license  {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ; Optional dependencies
                 [com.squareup.okhttp/okhttp "2.5.0" :scope "provided"]
                 [cheshire "5.5.0" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.285" :scope "provided" :exclusions [org.msgpack/msgpack]]
                 [com.cognitect/transit-cljs "0.8.237" :scope "provided"]]
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.7.170"]]
              :source-paths ["src" "examples/src"]
              :plugins [[lein-cljsbuild "1.1.2"]
                        [lein-doo "0.1.6"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :main 'happy.runner
                      :optimizations :whitespace
                      :pretty-print true}}}}
  :aliases {"clean-test" ["do" "clean," "test," "doo" "phantom" "test" "once"]
            "clean-install" ["do" "clean," "install"]
            "run-examples" ["do" "clean" ["run" "-m" "happy.examples"]]}
  :min-lein-version "2.5.0")
