(ns gif-captioner.sketch
  (:import [java.io File]
           [java.nio.file Paths Files]
           [gifAnimation Gif GifMaker])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [quil.core :as q]
            [quil.middleware :as qmw]))

(def *state
  (atom {:caption "POV: you are a yummy banana"
         :speed 100
         :progress 1}))

(defn- gif-delay
  "Get or set delay of gif."
  ([gif] (gif-delay gif nil))
  ([gif delay]
   (let [d (.. gif getClass (getDeclaredField "delays"))]
     (. d (setAccessible true))
     (if delay ; don't care about varying delays
       (. d (set gif (-> (. d (get gif))
                         alength
                         (repeat delay)
                         int-array)))
       (first (. d (get gif)))))))

(defn reverse-gif!
  "Reverse a GIF"
  [gif]
  (let [f (.. gif getClass (getDeclaredField "frames"))]
    (. f (setAccessible true))
    (. f (set gif (-> (. f (get gif)) reverse into-array)))))

(defn- split-by-text-width
  [strings max-width]
  (if (= (type strings) java.lang.String)
    (if (some #(= \newline %1) strings)
      (flatten (map #(split-by-text-width %1 max-width) (s/split-lines strings)))
      (split-by-text-width (s/split strings #" ") max-width))
    (->> (loop [strs strings
                acc [""]]
           (let [comb (str (first acc) (first strs))]
             (cond
               (empty? strs) acc
               (>= (q/text-width (first strs)) max-width) (recur (rest strs)
                                                         (concat [(first strs)] acc))
               (> (q/text-width comb) max-width) (recur strs (concat [""] acc))
               :else (recur (rest strs) (concat [(str comb " ")] (rest acc))))))
         (map s/trim)
         reverse)))

(defn- text-wrap
  "Sufficiently and predictably wrap text.
  Returns the height of the wrapped text."
  [string x y w]
  (let [asc (q/text-ascent)
        desc (q/text-descent)
        lh (+ asc desc)
        strs (split-by-text-width string w)]
    (dorun
     (for [i (range (count strs))
           :let [s (nth strs i)]]
       (q/text s (+ x (/ w 2)) (+ y asc (* lh i)))))))

(defn- text-height
  "Calculate height of text section from state."
  [state]
  ;; height = (2 × lines)pad + (text × lines)
  (+ (* (+ (q/text-ascent) (q/text-descent))
        (count
         (split-by-text-width
          (:caption state)
          (- (.width (:gif state)) (* 2 (:pad state))))))
     (* 2.5 (:pad state))))

(defn- get-resource
  "Stupid problems require stupid solutions.

  PFont and Gif constructors want filenames and can't read überjar
  resource urls, so spit them into tempdir and read them from there."
  [resource]
  (let [tmpdir (Paths/get (System/getProperty "java.io.tmpdir")
                          (into-array ^String ["gif-captioner"]))
        out-file (Paths/get (.toString tmpdir)
                            (into-array ^String [resource]))
        link-opts (make-array java.nio.file.LinkOption 0)]
    (when-not (Files/exists tmpdir link-opts)
      (.mkdirs (.toFile tmpdir)))
    (when-not (Files/exists out-file link-opts)
      (with-open [in (io/input-stream (io/resource resource))
                  out (io/output-stream (.toFile out-file))]
        (io/copy in out)))
    (.toString out-file)))

(defn- update-live [state]
  (let [a @*state
        state1 (assoc state
                      :caption (:caption a)
                      :speed (:speed a))]
    (gif-delay (:gif state1) (:speed state1))
    (if (= (:gif-file a) (:gif-file state1))
      state1
      (try
        (let [g (Gif. (quil.applet/current-applet) (:gif-file a))]
          (.loop g)
          (gif-delay g (gif-delay g))
          (swap! *state assoc
                 :pad (/ (.width g) 20)
                 :font (q/create-font (get-resource "caption.otf")
                                      (/ (.width g) 10))
                 :gif g
                 :gif-file (:gif-file a)
                 :speed (gif-delay g)))
        (catch Exception e
          (prn e)
          (swap! *state assoc :gif-file (:gif-file state1))
          state1)))))

(defn- update-print [state]
  (swap! *state assoc :progress
         (/ (+ (- (:total-frames state)
                  (count (:frames state)))
               1)
            (:total-frames state)))
  (update state :frames rest))

(defn- setup [initial-gif]
  (let [font (get-resource "caption.otf") ; (.getPath (io/resource "caption.otf"))
        gif-file (or initial-gif (get-resource "monke.gif")) ; (.getPath (io/resource "monke.gif"))
        gif (Gif. (quil.applet/current-applet) gif-file)]
    (gif-delay gif (gif-delay gif)) ; regularize gif delays
    (q/frame-rate 50)
    (.loop gif)
    {:caption "POV: you are a yummy banana"
     :speed (gif-delay gif)
     :progress 1
     :pad (/ (.width gif) 20)
     :gif gif
     :gif-file gif-file
     :font (q/create-font font (/ (.width gif) 10))}))

(defn- setup-live [initial-gif]
  ;; only mutate *state in live
  (reset! *state (setup initial-gif)))

(defn- setup-print [export-path]
  (-> (quil.applet/current-applet) .getSurface (.setVisible false))
  (let [export (GifMaker. (quil.applet/current-applet) export-path)
        at @*state
        state (assoc at :frames (seq (.getPImages (:gif at))))
        frame (first (:frames state))]
    (.setRepeat export 0)
    (q/text-font (:font state))
    (q/resize-sketch (.width frame) (+ (text-height state) (.height frame)))
    (assoc state
           :total-frames (count (:frames state))
           :export export)))

(defn- draw-state [state]
  (q/background 255)
  (q/fill 0)
  (q/text-font (:font state))
  (q/text-align :center)
  (let [pad (:pad state)
        w (.width (:gif state))]
    (text-wrap (:caption state) pad pad (- w (* 2 pad)))))

(defn- draw-live [state]
  (draw-state state)
  (let [gif (:gif state)
        w (.width gif)
        h (.height gif)]
    (q/image gif 0 (text-height state))
    (q/resize-sketch w (+ (text-height state) h))))

(defn- draw-print [state]
  (draw-state state)
  (let [frames (:frames state)
        ex (:export state)]
    (if (empty? frames)
      (do (.finish ex)
          (q/exit))
      (do (q/image (first frames) 0 (text-height state))
          (.setDelay ex (:speed state))
          (.addFrame ex)))))

(defn sketch-gif
  ([] (sketch-gif nil))
  ([initial-gif]
   (q/sketch ;; gif-captioner
     :title "GIF Preview"
     :size [10 10]
     :setup #(setup-live initial-gif)
     :update update-live
     :draw draw-live
     :features [:no-safe-fns :exit-on-close]
     :middleware [qmw/fun-mode])))

(defn create-gif
  "Create GIF from current *state."
  [export-path]
  (q/sketch
   :title "Creating GIF..."
   :size [10 10]
   :setup #(setup-print export-path)
   :draw draw-print
   :update update-print
   :features [:no-safe-fns]
   :middleware [qmw/fun-mode]))
