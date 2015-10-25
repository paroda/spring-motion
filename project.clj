(defproject spring-motion "0.1.0-SNAPSHOT"
  :description "An animation helper based on spring physics for clojurescript!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories [["clojars" {:sign-releases false}]]

  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                 [org.omcljs/om "0.9.0" :scope "provided"]
                 [sablono "0.3.6" :scope "provided"]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.1"]]

  :source-paths ["src/main"]

  :clean-targets ^{:protect false} ["resources/out"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/main"]
              :compiler {:asset-path "out"
                         :output-to "resources/out/app.js"
                         :output-dir "resources/out"
                         :optimizations :none}}
             ;; examples
             {:id "springs"
              :figwheel {:on-jsload "examples.springs/js-reload"}
              :source-paths ["src/main" "examples/springs/src"]
              :compiler {:main examples.springs
                         :asset-path "out"
                         :output-to "examples/springs/main.js"
                         :output-dir "examples/springs/out"
                         :source-map true
                         :optimizations :none}}
             {:id "springs-min"
              :source-paths ["src/main" "examples/springs/src"]
              :compiler {:main examples.springs
                         :asset-path "min"
                         :output-to "examples/springs/main.js"
                         :output-dir "examples/springs/min"
                         :optimizations :advanced}}
             {:id "draglist"
              :figwheel {:on-jsload "examples.draglist/js-reload"}
              :source-paths ["src/main" "examples/draglist/src"]
              :compiler {:main examples.draglist
                         :asset-path "out"
                         :output-to "examples/draglist/main.js"
                         :output-dir "examples/draglist/out"
                         :source-map true
                         :optimizations :none}}
             {:id "draglist-min"
              :source-paths ["src/main" "examples/draglist/src"]
              :compiler {:main examples.draglist
                         :asset-path "min"
                         :output-to "examples/draglist/main.js"
                         :output-dir "examples/draglist/min"
                         :optimizations :advanced}}
             {:id "staggered"
              :figwheel {:on-jsload "examples.staggered/js-reload"}
              :source-paths ["src/main" "examples/staggered/src"]
              :compiler {:main examples.staggered
                         :asset-path "out"
                         :output-to "examples/staggered/main.js"
                         :output-dir "examples/staggered/out"
                         :source-map true
                         :optimizations :none}}
             {:id "staggered-min"
              :source-paths ["src/main" "examples/staggered/src"]
              :compiler {:main examples.staggered
                         :asset-path "min"
                         :output-to "examples/staggered/main.js"
                         :output-dir "examples/staggered/min"
                         :optimizations :advanced}}
             {:id "ripples"
              :figwheel {:on-jsload "examples.ripples/js-reload"}
              :source-paths ["src/main" "examples/ripples/src"]
              :compiler {:main examples.ripples
                         :asset-path "out"
                         :output-to "examples/ripples/main.js"
                         :output-dir "examples/ripples/out"
                         :source-map true
                         :optimizations :none}}
             {:id "ripples-min"
              :source-paths ["src/main" "examples/ripples/src"]
              :compiler {:main examples.ripples
                         :asset-path "min"
                         :output-to "examples/ripples/main.js"
                         :output-dir "examples/ripples/min"
                         :optimizations :advanced}}
             {:id "todolist"
              :figwheel {:on-jsload "examples.todolist/js-reload"}
              :source-paths ["src/main" "examples/todolist/src"]
              :compiler {:main examples.todolist
                         :asset-path "out"
                         :output-to "examples/todolist/main.js"
                         :output-dir "examples/todolist/out"
                         :source-map true
                         :optimizations :none}}
             {:id "todolist-min"
              :source-paths ["src/main" "examples/todolist/src"]
              :compiler {:main examples.todolist
                         :asset-path "min"
                         :output-to "examples/todolist/main.js"
                         :output-dir "examples/todolist/min"
                         :optimizations :advanced}}]})
