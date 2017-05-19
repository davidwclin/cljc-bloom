(ns bloom.core-test
  #?(:clj  (:require [clojure.test :refer :all]
                     [clojure.java.io :as io]
                     [bloom.core :as c]
                     [clojure.data.json :as json]
                     )
     :cljs (:require [bloom.core :as c]
             [cljs.test :refer-macros [deftest is testing run-tests]]
             [goog.net.XhrIo :as xhr]
             [clojure.string :as str]
             )
     )
  )

#?(:cljs
   (enable-console-print!))

(defn- approx=
  [a b tolerance]
  (<= (Math/abs (- a b)) tolerance))

(defn- words-fp-count
  "run n times"
  [n bf]
  (loop [n   n
         cnt 0]
    (cond (= 0 n)
            cnt
          (c/has? bf (str n))
            (recur (dec n)
                   (inc cnt))
          :else
            (recur (dec n)
                   cnt))))

(defn words-fp-rate
  [n bf]
  (let [cnt (words-fp-count n bf)]
    (/ cnt n 1.0)))

;(defn eprintf [& args]
;  (.println System/err (apply format args)))

#?(:clj
   (def all-words
     (delay
       (let [f "resources/public/data/words.txt"]
         (with-open [rdr (io/reader f)]
           (doall (line-seq rdr))))))
   :cljs
   (defonce all-words
            (atom []))
   )

#?(:clj
   (defonce persisted-bf
     (delay
       (-> "resources/public/data/words-bf.json" slurp json/read-str)
         ))
   :cljs
   (defonce persisted-bf
            (atom nil))
   )

(deftest test-simple
  (testing "positives"
    (let [bf (c/->bf 100 0.01)]
      (c/add! bf "foo")
      (c/add! bf "bar")
      (is (c/has? bf "foo"))
      (is (c/has? bf "bar"))
      (is (not (c/has? bf "baz")))
      (is (not (c/has? bf "baz2")))
      ;(eprintf "to-string %s" (c/to-string bf))
      )
    )
  )

(deftest test-fp
  (testing "perf"
    (let [all-words @all-words
          target-fp-prob 0.04
          bf (c/->bf 200000 target-fp-prob :seed 0x811FC6)
          n 100000]
      (time
        (doseq [w all-words]
          (c/add! bf w)))
      ;#?(:clj (c/serialize bf "tmp/words-bf.json")) ;; for roundtrip testing
      (time
        (let [fp-rate (words-fp-rate n bf)]
          (println "fp rate is " fp-rate)
          (is (> target-fp-prob fp-rate)
              (str "FP rate times exceeded " (pr-str {:times n}) n))))
      (time
        (doseq [w (repeatedly 100 #(rand-nth all-words))]
          (is (c/has? bf w))))
      )
    )
  )

(deftest deserialize
 (testing "deserialize"
   (let [all-words @all-words
         target-fp-prob 0.04
         bf (c/->bf @persisted-bf)
         n 100000]
     (time
       (let [fp-rate (words-fp-rate n bf)]
         (println "fp rate is " fp-rate)
         (is (> target-fp-prob fp-rate)
             (str "FP rate times exceeded " (pr-str {:times n}) n))))
     (time
       (doseq [w (repeatedly 100 #(rand-nth all-words))]
         (is (c/has? bf w)))))
   ))

(deftest *size
  (is (approx= 10
               (let [bf (c/->bf 1000 0.01 :seed 0x811FC6)]
                 (dotimes [i 10]
                   (c/add! bf (str i)))
                 (c/size bf))
               0.1))
  (is (approx= 100
               (let [bf (c/->bf 1000 0.01 :seed 0x811FC6)]
                 (dotimes [i 100]
                   (c/add! bf (str i)))
                 (c/size bf))
               1))
  (is (approx= 1000
               (let [bf (c/->bf 1000 0.01 :seed 0x811FC6)]
                 (dotimes [i 1000]
                   (c/add! bf (str i)))
                 (c/size bf))
               11))
  (is (approx= 10000
               (let [bf (c/->bf 1000 0.01 :seed 0x811FC6)]
                 (dotimes [i 10000]
                   (c/add! bf (str i)))
                 (c/size bf))
               500))
  )

;; load all-words & persisted-bf on client
;; at bottom so tests are already loaded
#?(:cljs
   (do
     (if (empty? @all-words)
       (xhr/send "data/words.txt" (fn [e]
                                    (reset! all-words (-> e .-target .getResponse str/split-lines))
                                    (if-not @persisted-bf
                                      (xhr/send "data/words-bf.json" (fn [e]
                                                                       (reset! persisted-bf (-> e .-target .getResponseJson))
                                                                       (run-tests)))
                                      (run-tests))))
       (run-tests)))
   )

(comment
  (ns-unmap 'bloom.core-test 'persisted-bf)
  )