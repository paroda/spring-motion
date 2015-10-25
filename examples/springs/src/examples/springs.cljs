(ns examples.springs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [spring-motion.core :refer [spring motion]]
            [sablono.core :as html :refer-macros [html]]
            [om.core :as om]))

(enable-console-print!)
(println "Trace: Loading examples.springs")

(defonce app-state (atom {:row :k :col :r}))

(def spring-options {:k [50 100 300 500]
                     :r [40 30 20 15 10]})


;; test spring config
(defn app [data owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:style {:font "15px Arial"
                           :background-color "#eff"
                           :padding "10px"
                           :border "2px solid #0aa"}}
             [:h2 {:style {:margin-left "30px"}}
              (str "Spring behavior for combinations of Stiffness (k) & Damping (r)")]
             [:button {:style {:width 100
                               :height 30}
                       :on-click (fn []
                                   (om/transact! data #(assoc % :active? true)))}
              "Start"]
             [:button {:style {:width 100
                               :height 30
                               :margin-left 10}
                       :on-click (fn []
                                   (om/transact! data #(assoc % :active? false)))}
              "Reset"]
             [:button {:style {:margin-left 10
                               :height 30
                               :width 200}
                       :on-click (fn []
                                   (om/transact! data #(assoc %
                                                              :row (:col data)
                                                              :col (:row data))))}
              "Transpose row/column"]
             (let [{:keys [row col]} data]
               [:table {:style {:margin-top "30px"}}
                [:tbody
                 [:tr
                  [:th (str row " ~ " col)]
                  (map (fn [cv]
                         [:th (str col "= " cv)])
                       (col spring-options))]
                 (map
                  (fn [rv]
                    [:tr
                     [:th (str row "= " rv)]
                     (map
                      (fn [cv]
                        [:td {:style {:border "1px solid black"
                                      :width 200
                                      :height 100}}
                         (om/build
                          motion
                          {:default-style {:w 0}
                           :style (if (:active? data)
                                    {:w (spring 100 (if (= row :k) [rv cv] [cv rv]))}
                                    {:w 0})
                           :render
                           (fn [istyle]
                             (html [:div {:style {:background-color "#0aa"
                                                  :width (:w istyle)
                                                  :height 30}}]))}
                          {:react-key (str [rv cv])})])
                      (col spring-options))])
                  (row spring-options))]])]))))

(defn main []
  (om/root app app-state
           {:target (.getElementById js/document "app")}))

(main) ;; start

(defn js-reload []
  (main)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
