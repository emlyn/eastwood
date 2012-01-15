(ns eastwood.linters.unused
  (:require [clojure.set :as set]
            [eastwood.util :as util])
  (:use analyze.core analyze.util))

(defn unused-locals* [binding-expr]
  (let [lbs (util/local-bindings binding-expr)
        free (apply set/union (map util/free-locals
                                   (:children binding-expr)))]
    (set/difference lbs free)))

(defn binding-expr? [expr]
  (#{:let :let-fn :fn-method} (:op expr)))

(defmulti report (fn [expr locals] (:op expr)))

(defmethod report :let [expr locals]
  (if (> (count locals) 1)
    (println "Unused let-locals:" locals)
    (println "Unused let-local:" (first locals))))

(defmethod report :let-fn [expr locals]
  (if (> (count locals) 1)
    (println "Unused letfn arguments:" locals)
    (println "Unused letfn argument:" (first locals))))

(defmethod report :fn-method [expr locals]
  (if (> (count locals) 1)
    (println "Unused fn arguments:" locals)
    (println "Unused fn argument:" (first locals))))

(defn unused-locals [exprs]
  (doseq [expr (mapcat expr-seq exprs)]
    (when (binding-expr? expr)
      (when-let [ul (seq (for [{sym :sym} (unused-locals* expr)
                               :when (not= '_ sym)]
                           sym))]
        (report expr ul)))))

;; Unused private vars
(defn- private-defs [exprs]
  (->> (mapcat expr-seq exprs)
       (filter #(and (= :def (:op %))
                     (-> % :var meta :private)
                     (-> % :var meta :macro not))) ;; skip private macros
       (map :var)))
  
(defn- var-freq [exprs]
  (->> (mapcat expr-seq exprs)
       (filter #(= :var (:op %)))
       (map :var)
       frequencies))
  
(defn unused-private-vars [exprs]
  (let [pdefs (private-defs exprs)
        vfreq (var-freq exprs)]
    (doseq [pvar pdefs
            :when (nil? (vfreq pvar))]
      (println "Private var" pvar "is never used")))) 
