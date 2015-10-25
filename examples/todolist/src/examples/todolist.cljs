(ns examples.todolist
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [spring-motion.core :refer [spring transition-motion]]))

;;define app data
(defonce app-state (atom {}))

(defn handle-submit [owner e]
  (.preventDefault e)
  (om/update-state!
   owner
   (fn [{:keys [todos value] :as state}]
     (-> state
         (assoc-in [:todos (str "t" (js/Date.now))] {:text value :done? false})
         (assoc :value "")))))

(defn handle-change [owner e]
  (om/set-state! owner :value (-> e .-target .-value)))

(defn handle-done [owner date]
  (om/update-state! owner [:todos date :done?] not))

(defn handle-toggle-all [owner]
  (om/update-state!
   owner
   (fn [{:keys [todos] :as state}]
     (let [toggled (not (every? :done? (vals todos)))
           todos (reduce #(assoc-in % [%2 :done?] toggled)
                  todos (keys todos))]
       (assoc state :todos todos)))))

(defn handle-select [owner selected]
  (om/set-state! owner :selected selected))

(defn handle-clear-completed [owner]
  (om/update-state!
   owner :todos
   (fn [todos]
     (into {} (remove #(-> val :done?) todos)))))

(defn handle-destroy [owner date]
  (om/update-state! owner :todos #(dissoc % date)))

(defn default-value [owner]
  (let [{:keys [todos]} (om/get-state owner)]
    (reduce
     (fn [configs [date todo]]
       (assoc configs date {:height 0
                            :opacity 0
                            :data todo}))
     {} todos)))

(defn end-value [owner]
  (let [{:keys [todos value selected] :as state} (om/get-state owner)]
    (reduce
     (fn [configs [date todo]]
       (assoc configs date {:height (spring 60)
                            :opacity (spring 1)
                            :data todo}))
     {}
     (filter (fn [[date {:keys [text done?]}]]
               (and (<= 0 (.indexOf (.toUpperCase text) (.toUpperCase value)))
                    (or (and done? (= selected "completed"))
                        (and (not done?) (= selected "active"))
                        (= selected "all"))))
             todos))))

(defn will-enter [owner date]
  {:height 0
   :opacity 0
   :data (om/get-state owner [:todos date])})

(defn will-leave [owner date style-leaving]
  {:height (spring 0)
   :opacity (spring 0)
   :data (:data style-leaving)})

(defn app [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:todos {"t1" {:text "Board the plane" :done? false}
               "t2" {:text "Sleep" :done? false}
               "t3" {:text "Try to finish conference slides" :done? false}
               "t4" {:text "Eat cheese and drink wine" :done? false}
               "t5" {:text "Go round in Uber" :done? false}
               "t6" {:text "Talk with conf attendees" :done? false}
               "t7" {:text "Show Demo 1" :done? false}
               "t8" {:text "Show Demo 2" :done? false}
               "t9" {:text "Lament about the state of animation" :done? false}
               "t10" {:text "Show Secret Demo" :done? false}
               "t11" {:text "Go home" :done? false}}
       :value ""
       :selected "all"})
    om/IRenderState
    (render-state [_ {:keys [todos value selected]}]
      (let []
        (html
         [:section {:class-name "todoapp"}
          [:header {:class-name "header"}
           [:h1 "todos"]
           [:form {:on-submit (partial handle-submit owner)}
            [:input {:auto-focus true
                     :class-name "new-todo"
                     :placeholder "What needs to be done?"
                     :value value
                     :on-change (partial handle-change owner)}]]]
          [:section {:class-name "main"}
           [:input {:class-name "toggle-all"
                    :type "checkbox"
                    :on-change (partial handle-toggle-all owner)}]
           (om/build
            transition-motion
            {:default-styles (default-value owner)
             :styles (end-value owner)
             :will-leave (partial will-leave owner)
             :will-enter (partial will-enter owner)
             :render
             (fn [configs]
               (html
                [:ul {:class-name "todo-list"}
                 (map (fn [[date config]]
                        (let [style (dissoc config :data)
                              {:keys [text done?]} (:data config)]
                          [:li {:key date
                                :style style
                                :class-name (if done? "completed" "")}
                           [:div {:class-name "view"}
                            [:input {:class-name "toggle"
                                     :type "checkbox"
                                     :on-change (partial handle-done owner date)
                                     :checked done?}]
                            [:label text]
                            [:button {:class-name "destroy"
                                      :on-click (partial handle-destroy owner date)}]]]))
                      (sort-by key configs))]))})]
          [:footer {:class-name "footer"}
           [:span {:class-name "todo-count"}
            [:strong (count (remove #(-> % val :done?) todos))] " item left"]
           [:ul {:class-name "filters"}
            [:li {:key "all"}
             [:a {:class-name (if (= selected "all") "selected" "")
                  :on-click (partial handle-select owner "all")} "All"]]
            [:li {:key "active"}
             [:a {:class-name (if (= selected "active") "selected" "")
                  :on-click (partial handle-select owner "active")} "Active"]]
            [:li {:key "completed"}
             [:a {:class-name (if (= selected "completed") "selected" "")
                  :on-click (partial handle-select owner "completed")} "Completed"]]]
           [:button {:class-name "clear-completed"
                     :on-click (partial handle-clear-completed owner)} "Clear completed"]]])))))


(defn main []
  (om/root app app-state {:target (.getElementById js/document "app")}))

(main)

(defn js-reload []
  (main)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
