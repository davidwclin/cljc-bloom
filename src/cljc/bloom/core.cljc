(ns bloom.core
  #?@(:cljs [(:require [cljsjs.xxhash])
             ])
  #?@(:clj [(:refer-clojure :exclude [bit-shift-left bit-shift-right])
            (:require [clojure.data.json :as json])
            (:import [net.jpountz.xxhash XXHashFactory]
                     [bloom ThirtyTwoBit])
            ])
  )

;#?(:clj
;  (set! *warn-on-reflection* true))

(defn log [x]
  #?(:clj  (Math/log x)
     :cljs (js/Math.log x)))

(defn ceil [x]
  #?(:clj  (Math/ceil x)
     :cljs (js/Math.ceil x)))

(defn pow [a b]
  #?(:clj  (Math/pow a b)
     :cljs (js/Math.pow a b)))

#?(:clj
   (defn bit-shift-left [x n]
     (ThirtyTwoBit/bitShiftLeft x n)))

#?(:clj
   (defn bit-shift-right [x n]
     (ThirtyTwoBit/bitShiftRight x n)))


#?(:clj
   (defn xxhash32
     "returns 32-bit signed int"
     [^String s seed]
     (let [data (-> s (.getBytes "UTF-8"))
           hash32 (-> (XXHashFactory/fastestInstance) (.newStreamingHash32 seed))]
       (-> hash32 (.update data 0 (alength data))) ;;TODO: should buffer?
       (-> hash32 .getValue)
       ))
   )

#?(:cljs
   (defn xxhash32
     "returns 32-bit signed int"
     [s seed]
     (-> (js/XXH.h32 seed) (.update s) .digest .toNumber))
   )

(defprotocol IBloomFilter
  (locations [this v] "returns k indicies for corresponding v (value)")
  (add! [this v] "adds a v (value) to the bloom filter")
  (has? [this v] "checks if v (value) has been added to the bloom filter")
  (to-state [this] "returns state of the bloom filter")
  (serialize [this f] "serialize bloom filter to f (file)")
  (size [this] "estimated cardinality of bloom filter")
  )

(defn ->i
  "to bucket index"
  [l]
  #?(:clj  (-> l (/ 32) Math/floor long)
     :cljs (-> l (/ 32) js/Math.floor)
     ))

(defn popcnt [v]
  (let [v (as-> v $ (bit-shift-right $ 1) (bit-and $ 0x55555555) (- v $))
        v (+ (-> v (bit-and 0x33333333))
             (-> v (bit-shift-right 2) (bit-and 0x33333333)))
        v (-> v (bit-shift-right 4) (+ v) (bit-and 0xf0f0f0f))]
    (-> v (* 0x1010101)  (bit-shift-right 24))))

(deftype BloomFilter
  ;; buckets- => mutable int32 array
  ;; m => # of bits
  ;; k => # of hashes
  [m k ^ints buckets- seed]
  IBloomFilter
  (locations [_ v]
    (let [a (xxhash32 v seed)
          b (xxhash32 (str v seed) seed)]
      (map #(-> a (* %) (+ b) (mod m)) (range 0 k))))
  (add! [this v]
    (let [ls (-> (locations this v))]
      (doseq [l ls
              :let [i (->i l)
                    v (-> (aget buckets- i)
                          (bit-or (bit-shift-left 1 (mod l 32)))
                          )]]
        (aset buckets- i v)
        )))
  (has? [this v]
    (let [ls (locations this v)]
      (every? (fn [l]
                (let [i (->i l)]
                  (-> (aget buckets- i)
                      (bit-and (bit-shift-left 1 (mod l 32)))
                      (not= 0)))) ls)))
  (to-state [_]
    {:buckets- (vec buckets-)
     :seed seed
     :m m
     :k k})
  (serialize [this f]
    #?(:clj  (-> this to-state json/write-str (spit f))
       :cljs (-> this to-state clj->js js/JSON.stringify (#(str "data:text/json;charset=utf-8," %)) js/encodeURI window.open)
       ))
  (size [_]
    (let [bits (->> buckets- (map popcnt) (reduce +))]
      (as-> bits $ (/ $ m) (- 1 $) (Math/log $) (* $ (- m)) (/ $ k))))
  )

;; via https://github.com/kyleburton/clj-bloom/blob/master/src/com/github/kyleburton/clj_bloom.clj

(defn optimal-m
  "returns optimal size of bit array"
  [cardinality fp-prob]
  (-> (* -1
        (/ (* cardinality (log fp-prob))
           (pow (log 2) 2)))
      (/ 32) ceil (* 32) long))

(defn optimal-k
  "returns optimal number of hashes"
  [cardinality m-bits]
  (-> (* (/ m-bits cardinality)
         (log 2))
      ceil long))

(defn ->bf
  "created bf from scratch or previously serialized state."
  ([cardinality fp-prob & {:keys [seed]
                           :or {seed 0x811CC5}}]

   (let [m (optimal-m cardinality fp-prob)
         k (optimal-k cardinality m)
         n (-> m (/ 32) ceil int)
         buckets- (int-array n 0)]
     (BloomFilter. m k buckets- seed)))
  ;; couple possibilities:
  ;;   clj  => state via `json/read-string` gives a clj map, need to change buckets- into a Java array
  ;;   cljs => state is an js Object, need to be a cljs map & keep buckets- native.
  ([state]
   (let [state #?(:clj  state
                  :cljs (if (map? state) state ;; already a cljc map
                          (let [ks (js/Object.keys state)]
                            (zipmap ks (map #(aget state %) ks)))))
         {:strs [buckets- seed m k]} state
         buckets- (if (not (coll? buckets-)) buckets- ;; do nothing if already native
                    #?(:clj  (into-array Integer/TYPE buckets-)
                       :cljs (into-array              buckets-)))]
     (assert (not (empty? buckets-)))
     (BloomFilter. m k buckets- seed)))
  )
