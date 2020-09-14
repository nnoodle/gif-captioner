(defproject gif-captioner "0.1.1-SNAPSHOT"
  :description "make those funny haha gifs that those people keep spamming"
  :url "http://github.com/nnoodle/gif-captioner"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cljfx "1.7.8" :exclusions [org.openjfx/javafx-web]]
                 [quil "3.1.0"]
                 [local/gifAnimation "3.0.0"]]
  :repositories {"project" {:url "file:repo"
                            :checksum :ignore}}
  :main ^:skip-aot gif-captioner.core
  :target-path "target/%s"
  :jar-name "gif-captioner.jar"
  :uberjar-name "gif-captioner-standalone.jar"
  :clean-targets [:target-path "pom.xml"]
  :profiles {:uberjar {:aot :all
                       :omit-source true
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dcljfx.skip-javafx-initialization=true"]}})
