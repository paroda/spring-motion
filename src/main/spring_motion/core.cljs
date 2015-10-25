(ns spring-motion.core
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]])
  (:require [spring-motion.spring :refer [simulate-style simulate-styles]]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! <! chan mult tap untap close! sliding-buffer]]))

 ;; pub-sub channel
(defonce ^:private pub-chan (chan (sliding-buffer 1)))
(defonce ^:private mult-chan (mult pub-chan))

;; hook unto animation cycle of window
(defn- hook-animation
  ([]
   (.requestAnimationFrame js/window hook-animation))
  ([timestamp]
   (.requestAnimationFrame js/window hook-animation)
   (put! pub-chan {:tick timestamp})))
(defonce ^:private hook (hook-animation)) ;; start only once

;; spring
(defn spring
  "The utility to specify spring settings.
  target: the final value at the end of animation
  [stiffness damping]: optional, default [50 10]"
  ([target]
   ^:spring {:target target
              :stiffness 170
              :damping 26})
  ([target [stiffness damping]]
   ^:spring {:target target
              :stiffness stiffness
              :damping damping}))



;; define component builder for motion
(defn motion
  "Returns a om component to animate the contents using given spring specifications.
  usage: to be used with om/build or its variants, for example
         (om/build motion data)
where
  data: {:default-style default-style
         :style style
         :render render}
  default-style: optional, map of styles like {:x 0 :y 0 :z 8}
  style: required, map of target styles, for example
         {:x (spring 10) :y (spring 5 [1 1]) :z 8}
  render: required, a function (fn [style] ...) that takes the interpolated style and
          returns the content to render.
when the default-style is not specified, it will directly render the final style.

example:
  (om/build motion {:default-style {:w 0}
                    :style {:w (spring 10)}
                    :render (fn [s] (sablono/html [:div {:style {:width (:w s)}}]))})"
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:t-style (:style data)
       :i-style (:default-style data)
       :i-speed nil
       :done? false
       :last-tick 0
       :tick-chan (tap mult-chan (chan))
       :prop-chan (chan)})
    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (put! (om/get-state owner :prop-chan) {:t-style (:style next-props)}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [tick-chan prop-chan]} (om/get-state owner)]
        (go-loop []
          (when-let [e (alt!
                         tick-chan ([t] t)
                         prop-chan ([p] p))]
            (cond
              ;; reset the target style
              (:t-style e)
              (om/update-state! owner #(assoc % :done? false
                                              :t-style (:t-style e)))
              ;; animate for next tick
              (:tick e)
              (let [tick (:tick e)
                    {:keys [t-style i-style i-speed done? last-tick]}
                    (om/get-state owner)]
                (if (not done?)
                  (if (= last-tick 0)
                    ;; initialize tick and wait for next cycle
                    (om/update-state! owner #(assoc % :last-tick tick))
                    ;; animate
                    (let [[i-style i-speed done?]
                          (simulate-style t-style i-style i-speed (- tick last-tick))]
                      (om/update-state! owner #(assoc %
                                                      :last-tick (if done? 0 tick)
                                                      :i-style i-style
                                                      :i-speed i-speed
                                                      :done? done?)))))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [tick-chan prop-chan]} (om/get-state owner)]
        (untap mult-chan tick-chan)
        (close! tick-chan)
        (close! prop-chan)))
    om/IRenderState
    (render-state [_ {:keys [i-style]}]
      (if-let [render (:render (om/get-props owner))]
        (render i-style)))))


;; define component builder for staggered-motion
(defn staggered-motion
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:t-styles (:styles data)
       :i-styles (:default-styles data)
       :i-speeds {}
       :done? false
       :last-tick 0
       :tick-chan (tap mult-chan (chan))
       :prop-chan (chan)})
    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (put! (om/get-state owner :prop-chan) {:t-styles (:styles next-props)}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [tick-chan prop-chan]} (om/get-state owner)]
        (go-loop []
          (when-let [e (alt!
                         tick-chan ([t] t)
                         prop-chan ([p] p))]
            (cond
              ;; reset the target style
              (:t-styles e)
              (om/update-state! owner #(assoc % :done? false
                                              :t-styles (:t-styles e)))
              ;; animate for next tick
              (:tick e)
              (let [tick (:tick e)
                    {:keys [t-styles i-styles i-speeds done? last-tick]}
                    (om/get-state owner)]
                (if (not done?)
                  (if (= last-tick 0)
                    ;; initialize tick and wait for next cycle
                    (om/update-state! owner #(assoc % :last-tick tick))
                    ;; animate
                    (let [n-styles (if (map? t-styles) t-styles (t-styles i-styles))
                          [i-styles i-speeds done-keys]
                          (simulate-styles n-styles i-styles i-speeds (- tick last-tick))
                          done? (= (not-empty done-keys) (not-empty (keys i-styles)))
                          done? (if (or (not done?) (map? t-styles)) done?
                                    (= n-styles (t-styles i-styles)))]
                      (om/update-state! owner #(assoc %
                                                      :last-tick (if done? 0 tick)
                                                      :i-styles i-styles
                                                      :i-speeds i-speeds
                                                      :done? done?)))))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [tick-chan prop-chan]} (om/get-state owner)]
        (untap mult-chan tick-chan)
        (close! tick-chan)
        (close! prop-chan)))
    om/IRenderState
    (render-state [_ {:keys [i-styles]}]
      (if-let [render (om/get-props owner :render)]
        (render i-styles)))))

;; define component builder for transition-motion
(defn transition-motion
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:t-styles (:styles data)
       :i-styles (:default-styles data)
       :i-speeds {}
       :to-remove {}
       :done? false
       :tick 0
       :tick-chan (tap mult-chan (chan))
       :prop-chan (chan)})
    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (put! (om/get-state owner :prop-chan) {:t-styles (:styles next-props)}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [tick-chan prop-chan]} (om/get-state owner)]
        (go-loop []
          (when-let [e (alt!
                         tick-chan ([t] t)
                         prop-chan ([p] p))]
            (cond
              ;; update styles target
              (:t-styles e)
              (om/update-state! owner #(assoc % :done? false
                                              :t-styles (:t-styles e)))
              ;; animate next iteration
              (:tick e)
              (let [tick (:tick e)
                    {:keys [t-styles i-styles i-speeds to-remove done? last-tick]}
                    (om/get-state owner)]
                (if (not done?)
                  (if (= last-tick 0)
                    ;; initialize tick and wait for next cycle
                    (om/update-state! owner #(assoc % :last-tick tick))
                    ;; animate
                    (let [n-styles (if (map? t-styles) t-styles (t-styles i-styles))
                          {:keys [will-enter will-leave]} (om/get-props owner)
                          ;;proecess addition
                          [i-styles to-remove]
                          (reduce (fn [[i-styles to-remove] [k n-style]]
                                    [(if (or (get i-styles k) (not will-enter)) i-styles
                                         (assoc i-styles k (will-enter k n-style n-styles)))
                                     (if (get to-remove k) (dissoc to-remove k) to-remove)])
                                  [i-styles to-remove] n-styles)
                          ;;process removal
                          [i-styles to-remove]
                          (reduce (fn [[i-styles to-remove] [k i-style]]
                                    [(if (or (get n-styles k) will-leave) i-styles
                                         (dissoc i-styles k))
                                     (if (and (not (get n-styles k)) will-leave)
                                       (assoc to-remove k (will-leave k i-style n-styles))
                                       (dissoc to-remove k))])
                                  [i-styles to-remove] i-styles)
                          ;;animate one step
                          [i-styles i-speeds done-keys]
                          (simulate-styles (merge n-styles to-remove)
                                           i-styles i-speeds (- tick last-tick))
                          done? (= (not-empty done-keys) (not-empty (keys i-styles)))
                          ;;complete removal for completed animations
                          [i-styles to-remove]
                          (reduce (fn [[i-styles to-remove] k]
                                    [(dissoc i-styles k) (dissoc to-remove k)])
                                  [i-styles to-remove]
                                  (filter #(get to-remove %) done-keys))
                          done? (if (or (not done?) (map? t-styles)) done?
                                    (= n-styles (t-styles i-styles)))]
                      (om/update-state! owner #(assoc %
                                                      :last-tick (if done? 0 tick)
                                                      :i-styles i-styles
                                                      :i-speeds i-speeds
                                                      :done? done?)))))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (let [tick-chan (om/get-state owner :tick-chan)]
        (untap mult-chan tick-chan)
        (close! tick-chan)))
    om/IRenderState
    (render-state [_ {:keys [i-styles]}]
      (if-let [render (om/get-props owner :render)]
        (render i-styles)))))
