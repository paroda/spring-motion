(ns examples.ripples
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [spring-motion.core :refer [spring transition-motion]]))

;;define app data
(defonce app-state (atom {}))

(defn app [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:mouse []
       :now "t0"})
    om/IRenderState
    (render-state [_ {[mouse-x mouse-y] :mouse now :now}]
      (let [styles (if (not mouse-x) {}
                       {now {:opacity (spring 1)
                             :scale (spring 0)
                             :x (spring mouse-x)
                             :y (spring mouse-y)}})]
        (om/build
         transition-motion
         {:styles styles
          :will-leave (fn [k s] (assoc s
                                       :opacity (spring 0 [60 15])
                                       :scale (spring 2 [60 15])))
          :render
          (fn [circles]; (js/console.log (str circles))
            (html
             [:div {:on-mouse-move (fn [e]
                                     (om/set-state! owner
                                                    {:mouse [(- (.-pageX e) 25)
                                                             (- (.-pageY e) 25)]
                                                     :now (str "t" (js/Date.now))}))
                    :className "ripples"}
              (map (fn [[k cs]]
                     (let [{:keys [opacity scale x y]} cs
                           transform (str "translate3d(" x "px, " y "px, 0) scale(" scale ")")]
                       [:div {:key k
                              :className "ripples-ball"
                              :style {:opacity opacity
                                      :scale scale
                                      :transform transform
                                      :-webkit-transform transform}}]))
                   circles)]))})))))


(defn main []
  (om/root app app-state {:target (.getElementById js/document "app")}))

(main)

(defn js-reload []
  (main)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
