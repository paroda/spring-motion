(ns spring-motion.spring)

(def ^:private tolerance 1e-3)

;; spring simulation, return new velocity and position
(defn simulate-spring
  "Returns new velocity and stretch [v x] of the spring for given
  v: old velocity
  x: old stretch
  k: stiffness of spring
  r: resistance or damping of spring
  t: interval, milliseconds
  
For simplicity mass just considered as 1 in all calculations. You can adjust
the stiffness and damping to get a desired spring behavior."
  [v x k r t]
  (let [t (* t 1e-3)
        f (- (* k x))
        d (- (* r v))
        a (+ f d)
        v (+ v (* a t))
        x (+ x (* v t))]
    [v x]))

(defn simulate-spring-2
  "Same as simulate-spring except that it can handle large time steps"
  [v x k r t]
  ;(println "spring simul " [v x])
  (let [k (* k 1)  ;; adjust scaling
        r (* r 1)  ;; adjust scaling
        t (* t 0.005) ;; adjust scaling
        w (js/Math.sqrt k) ;natural frequency
        c (/ r w)]         ;damping ratio
    (cond
      (= c 1) ;; critical damping
      (let [A x
            B (+ v (* w x))
            A+Bt (+ A (* B t))
            e_-wt (js/Math.exp (* (- w) t))
            x (* A+Bt e_-wt)
            v (* (- B (* w A+Bt)) e_-wt)]
        [v x])
      (> c 1) ;; over-damping
      (let [a (- (* c w))
            b (* w (js/Math.sqrt (- (* c c) 1)))
            r+ (+ a b)
            r- (- a b)
            B (/ (- (* r+ x) v) (- r+ r-))
            A (- x B)
            e_r+*t (js/Math.exp (* r+ t))
            e_r-*t (js/Math.exp (* r- t))
            x (+ (* A e_r+*t) (* B e_r-*t))
            v (+ (* r+ A e_r+*t) (* r- B e_r-*t))]
        [v x])
      (< c 1) ;; under-damping
      (let [f (* w (js/Math.sqrt (- 1 (* c c)))) ; natural damped frequency
            A x
            B (/ (+ (* c w x) v) f)
            ft (* f t)
            e_-cwt (js/Math.exp (- (* c w t)))
            cos_ft (js/Math.cos ft)
            sin_ft (js/Math.sin ft)
            x (* e_-cwt (+ (* A cos_ft) (* B sin_ft)))
            v (- (* e_-cwt (- (* B f cos_ft) (* A f sin_ft))) (* c w x))]
        [v x]))))

;; target value of given style
(defn style-target [style]
  (reduce (fn [s [k v]]
            (assoc s k
                   (if (-> v meta :spring)
                     (:target v)
                     v)))
          {} style))

;; simulate a single style
;; returns next style/speed and if completed
(defn simulate-style [t-style i-style i-speed delta]
  (reduce
   (fn [[i-style* i-speed* done?] [k spring-or-target]]
     (if (-> spring-or-target meta :spring)
       (let [{:keys [target stiffness damping]} spring-or-target
             v (or (get i-speed k) 0)
             x (- (or (get i-style k) target) target)]
         (if (= [v x] [0 0])
           [(assoc i-style* k target)
            (assoc i-speed* k 0)
            done?]
           (let [[v x]
                 (mapv #(if (< (js/Math.abs %) tolerance) 0 %)
                       ((if (> delta 50) simulate-spring-2 simulate-spring)
                        v x stiffness damping delta))]
             [(assoc i-style* k (+ x target))
              (assoc i-speed* k v)
              (and done? (= [v x] [0 0]))])))
       [(assoc i-style* k spring-or-target)
        (assoc i-speed* k 0)
        done?]))
   [{} {} true] t-style))

;; simulate multiple styles
;; returns next styles/speeds and keys of completed styles
(defn simulate-styles [t-styles i-styles i-speeds delta]
  (reduce
   (fn [[i-styles* i-speeds* done] [k t-style]]
     (let [i-style (get i-styles k)
           i-speed (get i-speeds k)
           [i-style i-speed done?]
           (if (and (= i-style (style-target t-style))
                    (empty? (remove #(= 0 (val %)) i-speed)))
             [i-style i-speed true] ;; spring at rest
             (simulate-style t-style i-style i-speed delta))]
       [(assoc i-styles* k i-style)
        (assoc i-speeds* k i-speed)
        (if done? (conj done k))]))
   [{} {} []] t-styles))
