(ns examples.staggered
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <! pub sub sliding-buffer close!]]
            [spring-motion.core :refer [spring staggered-motion]]))

;; define app data
(def ball-count 6)
(defonce app-state (atom {}))

;; event-handler multi-method
(defmulti handle-event :tag)

(defmethod handle-event :mouse-move [evt]
  (let [{:keys [owner page-x page-y]} evt]
      (om/update-state! owner #(assoc % :x page-x :y page-y))))


;; define component
(defn app [data owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [{events-in :notif-chan events-out :pub-chan} (om/get-shared owner)
            events-in (sub events-in :app (chan (sliding-buffer 1)))
            app-mouse-move (fn appMouseMove [e]
                             (put! events-out {:notify :app
                                               :tag :mouse-move
                                               :owner owner
                                               :page-y (.-pageY e)
                                               :page-x (.-pageX e)}))]
        {:x 250
         :y 300
         :events-in events-in
         :event-listeners {:mouse-move ["mousemove" app-mouse-move]}}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [events-in event-listeners]} (om/get-state owner)]
        (go
          (loop []
            (when-let [e (<! events-in)]
              (handle-event e)
              (recur))))
        (doall
         (map (fn [[etag ehandler-fn]]
                (.addEventListener js/window etag ehandler-fn))
              (vals event-listeners)))))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [events-in event-listeners]} (om/get-state owner)]
           (close! events-in)
           (doall
            (map (fn [[etag ehandler-fn]]
                   (.removeEventListener js/window etag ehandler-fn))
                 (vals event-listeners)))))
    om/IRender
    (render [_]
      (om/build
       staggered-motion
       {:default-styles (reduce (fn [s i]
                                  (assoc s (str "ball-" i) {:x 100 :y 100}))
                                {} (range ball-count))
        :styles (fn [pstyles]          ;(js/console.log (str pstyles))
                  (reduce
                   (fn [s i]
                     (if (= 0 i)
                       (let [{:keys [x y]} (om/get-state owner)]
                         (assoc s "ball-0"
                                {:x x #_(spring x [100 10])
                                 :y y #_(spring y [100 10])}))
                       (let [{:keys [x y]} (get pstyles (str "ball-" (dec i)))]
                         (assoc s (str "ball-" i)
                                {:x (spring x [50 15])
                                 :y (spring y [50 15])}))))
                   {} (range ball-count)))
        :render
        (fn [styles]
          (html
           [:div {:className "staggered"}
            (map (fn [i]
                   (let [{:keys [x y]} (get styles (str "ball-" i))]
                     [:div {:className "staggered-ball"
                            :key i
                            :style (let [s (str "translate3d(" (- x 25) "px, "
                                                (- y 25) "px, 0)")]
                                     {:-webkit-transform s
                                      :transform s
                                      :z-index (- ball-count i)})}
                      [:h3 i]]))
                 (range ball-count))]))}))))


(defn main []
  (let [req-chan (chan)
        pub-chan (chan)
        notif-chan (pub pub-chan :notify)]
    (om/root app app-state {:shared {:req-chan req-chan
                                     :notif-chan notif-chan
                                     :pub-chan pub-chan}
                            :target (.getElementById js/document "app")})))

(main)

(defn js-reload []
  (main)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
