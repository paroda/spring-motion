(ns examples.draglist
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <! >! pub sub sliding-buffer close!]]
            [spring-motion.core :refer [spring motion]]))

;; define app data
(def items-count 4)
(defonce app-state (atom {:spring-config [50 10]}))

;; utility functions

(defn indices [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))
(defn index-of [coll val]
  (first (indices #(= % val) coll)))

(defn reinsert [vec from to]
  (cond
    (= from to) vec
    (> from to) (into []
                      (concat (take to vec)
                              [(nth vec from)]
                              (take (- from to) (drop to vec))
                              (drop (inc from) vec)))
    (< from to) (into []
                      (concat (take from vec)
                              (take (- to from) (drop (inc from) vec))
                              [(nth vec from)]
                              (drop (inc to) vec)))))
(defn clamp [n min max]
  (Math/max min (Math/min n max)))

;; event-handler multi-method
(defmulti handle-event :tag)

(defmethod handle-event :mouse-down [evt]
  (let [{:keys [owner key y page-y]} evt]
    (om/update-state! owner #(merge % {:delta (- page-y y)
                                       :mouse y
                                       :is-pressed true
                                       :last-pressed key}))))

(defmethod handle-event :mouse-move [evt]
  (let [{:keys [owner page-y]} evt]
    (if (om/get-state owner :is-pressed)
      (om/update-state!
       owner
       (fn [state]
         (let [{:keys [delta last-pressed order]} state
               mouse (- page-y delta)
               row (clamp (Math.round (/ mouse 100)) 0 (dec items-count))
               new-order (reinsert order (index-of order last-pressed) row)]
           (assoc state :mouse mouse :order new-order)))))))

(defmethod handle-event :mouse-up [evt]
  (let [{:keys [owner]} evt]
    (om/update-state! owner #(assoc % :is-pressed false :delta 0))))

;; define component
(defn app [data owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [{events-in :notif-chan events-out :pub-chan} (om/get-shared owner)
            events-in (sub events-in :app (chan (sliding-buffer 1)))
            app-mouse-up (fn appMouseUp []
                          (put! events-out {:notify :app
                                            :tag :mouse-up
                                            :owner owner}))
            app-mouse-move (fn appMouseMove [e]
                            (put! events-out {:notify :app
                                              :tag :mouse-move
                                              :owner owner
                                              :page-y (.-pageY e)}))]
        {:delta 0
         :mouse 0
         :is-pressed false
         :last-pressed 0
         :order (range items-count)
         :events-in events-in
         :event-listeners {:mouse-up ["mouseup" app-mouse-up]
                           :mouse-move ["mousemove" app-mouse-move]}}))
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
    om/IRenderState
    (render-state [_ {:keys [mouse is-pressed last-pressed order]}]
      (let [{events-out :pub-chan} (om/get-shared owner)
            spring-config (:spring-config data)]
        (html
         [:div {:className "app"}
          (map (fn [i]
                 (let [style (if (and is-pressed (= last-pressed i))
                               {:scale (spring 1.1 spring-config)
                                :shadow (spring 16 spring-config)
                                :y mouse}
                               {:scale (spring 1 spring-config)
                                :shadow (spring 1 spring-config)
                                :y (spring (* 100 (index-of order i)) spring-config)})]
                   (om/build
                    motion
                    {:style style
                     :render
                     (fn [{:keys [scale shadow y]}]
                       (html
                        [:div {:on-mouse-down
                               (fn appMouseDown [e]
                                 (put! events-out {:notify :app
                                                   :tag :mouse-down
                                                   :owner owner
                                                   :key i
                                                   :y y
                                                   :page-y (.-pageY e)})
                                 (.preventDefault e))
                               :class-name "app-item"
                               :style {:box-shadow (str "rgba(0, 0, 0, 0.2) 0px "
                                                        shadow "px "
                                                        (* 2 shadow) "px 0px")
                                       :transform (str "translate3d(0, " y
                                                       "px, 0) scale(" scale ")")
                                       :-webkit-transform (str "translate3d(0, " y
                                                               "px, 0) scale(" scale ")")
                                       :z-index (if (= i last-pressed) 99 i)}}
                         (inc i)]))}
                    {:react-key i})))
               (range items-count))])))))

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

