(defproject gif-captioner "0.1.0-SNAPSHOT"
  :description "make those funny haha gifs that people keep spamming"
  :url "http://github.com/nnoodle/gif-captioner"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cljfx "1.7.8"]
                 [quil "3.1.0"]
                 [local/gifAnimation "3.0.0"]]
  :repositories {"project" "file:repo"}
  :main ^:skip-aot gif-captioner.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all ;; [gif-captioner.core]
                       :omit-source true
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dcljfx.skip-javafx-initialization=true"]}})
