# cljc-bloom

A cross-platform [bloom filter](https://en.wikipedia.org/wiki/Bloom_filter) implemented in Clojure(Script).

## Installation

Include the following dependency in project.clj file:

    :dependencies [[davidwclin/cljc-bloom "0.1.1"]]

## Example Usage
```clojure
    (require '[bloom.core :as bf]) 
    (let [upper-cardinality 20
          target-false-positive-rate 0.05]
      (def my-bf
        (bf/->bf upper-cardinality target-false-positive-rate)))
    (doseq [i (range 10)]
      (bf/add! my-bf (str i)))
    (bf/has? my-bf "4")  ;; true
    (bf/has? my-bf "45") ;; false
```

(de)serialization into (or from) JSON    
```clojure
    ;; serialize in clj
    (bf/serialize my-bf "/tmp/my-bf.json")
    
    ;; deserialize in clj
    (require '[clojure.data.json :as json])
    (def my-bf
      (-> "/tmp/my-bf.json" slurp json/read-str bf/->bf))
    (bf/has? my-bf "4")  ;; true
    (bf/has? my-bf "45") ;; false
    
    ;; deserialize in cljs
    ;; retrieve serialized bf from server. Using just the file content as an example here.
    (let [serialized-string "{\"buckets-\":[174137362,276431338,1342183560,-1029155791],\"seed\":8461509,\"m\":128,\"k\":5}"]
      (def my-bf
        (-> serialized-string js/JSON.parse bf/->bf)))
    (bf/has? my-bf "4")  ;; true
    (bf/has? my-bf "45") ;; false
```
## Implementation

* Bloom filters require an arbitrary number of hashes, this is achieved by generating linear combinations
  of two [xxHashes](http://cyan4973.github.io/xxHash/).
* Javscript bitwise ops are [32 bits](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Bitwise_Operators),
  so the same is done on the Java side for compatibility. 

## Inspirations

* https://www.jasondavies.com/bloomfilter
* https://github.com/kyleburton/clj-bloom

## Development 

* cljs - [run in cursive](https://github.com/bhauman/lein-figwheel/wiki/Running-figwheel-in-a-Cursive-Clojure-REPL)
* clj - run as usual

## License

Copyright © 2017 @davidwclin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
