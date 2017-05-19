(defproject davidwclin/cljc-bloom "0.1.1"
  :description "a cross-platform bloom filter for clojure(script)"
  :url "https://github.com/davidwclin/cljc-bloom"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.3"
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [cljsjs/xxhash "0.2.1-0"]
                 [org.clojure/data.json "0.2.6"]
                 ]
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.0-6"]]
                   }}
  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.5.0-6"]]
  :source-paths ["src/cljc"]
  :java-source-paths ["src/java"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["script"]
                        :figwheel true
                        :compiler {
                          :main bloom.core-test
                          :asset-path "js/compiled/out"
                          :output-to  "resources/public/js/compiled/bloom_dev.js"
                          :output-dir "resources/public/js/compiled/out"
                          :source-map-timestamp true
                         }}]}
            )



