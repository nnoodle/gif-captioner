(ns gif-captioner.core
  (:gen-class)
  (:import [javafx.application Platform]
           [javafx.stage FileChooser FileChooser$ExtensionFilter]
           [javafx.event ActionEvent]
           [javafx.scene Node])
  (:require [cljfx.api :as fx]
            [cljfx.ext.node :as fx.ext.node]
            [gif-captioner.sketch :as sketch]))

(defmulti event-handler :event/type)

(defn- slider-view [{:keys [^Integer speed]}]
  :fx/type fx.ext.node/with-tooltip-props
  :props {:tooltip {:fx/type :tooltip
                    :text (str "Delay between frames in milliseconds. "
                               "(smaller is faster)")}}
  :desc {:fx/type :slider
         :min 20
         :max 200
         :snap-to-ticks true
         :major-tick-unit 20
         :show-tick-labels true
         :show-tick-marks true
         :on-value-changed {:event/type ::set-delay}
         :value speed})

(defn- root-view [{{:keys [caption speed progress] :as state} :state}]
  {:fx/type :stage
   :showing true
   :title "GIF Captioner"
   :on-close-request (fn [_] (System/exit 0))
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :padding 20
                  :spacing 3
                  :children [{:fx/type :label
                              :text "Caption Text"}
                             {:fx/type :text-area
                              :pref-row-count 5
                              :pref-column-count 20
                              :text caption
                              :on-text-changed {:event/type ::set-caption}
                              :prompt-text "GIF Caption…"}
                             {:fx/type :label
                              :text (str "Delay in ms (" speed "ms)")}
                             {:fx/type slider-view
                              :speed speed}
                             {:fx/type :h-box
                              :alignment :center-left
                              :spacing 10
                              :padding {:top 5}
                              :children [{:fx/type :button
                                          :text "Open GIF"
                                          :on-action {:event/type ::pick-gif
                                                      :mode :open}}
                                         {:fx/type :button
                                          :text "Export GIF"
                                          :disable (not (== 1 progress))
                                          :on-action {:event/type ::pick-gif
                                                      :mode :export}}
                                         {:fx/type :progress-bar
                                          :visible (not (== 1 progress))
                                          :progress progress}]}]}}})

(defmethod event-handler ::pick-gif [{:keys [^ActionEvent fx/event mode]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        picker (doto (FileChooser.)
                 (.setTitle (if (= mode :open)
                              "Open GIF"
                              "Export GIF")))]
    (-> picker
        .getExtensionFilters
        (.add (FileChooser$ExtensionFilter. "GIF Files" '("*.gif"))))
    (when-let [file @(fx/on-fx-thread
                      (if (= mode :open)
                        (.showOpenDialog picker window)
                        (.showSaveDialog picker window)))]
      (if (= mode :open)
        (swap! sketch/*state assoc :gif-file (.getPath file))
        (sketch/create-gif (.getPath file))))))

(defmethod event-handler ::set-delay [{^Double ms :fx/event}]
  (swap! sketch/*state assoc :speed (Math/round ms)))

(defmethod event-handler ::set-caption [{^String s :fx/event}]
  (swap! sketch/*state assoc :caption s))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state] {:fx/type root-view
                                              :state state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(defn -main [& _args]
  (Platform/setImplicitExit true)
  (sketch/sketch-gif)
  (fx/mount-renderer sketch/*state renderer))
