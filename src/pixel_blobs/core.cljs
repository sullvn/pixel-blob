(ns pixel-blobs.core
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [clojure.set :as s]))


; Constants
(def width 700)
(def height 600)
(def cell-size 8)
(def max-blob-size 500)
(def lavender-color [256 0.47 0.65])
(def background-color [256 0 0.65])


; Derived Constants
(def max-x (quot width cell-size))
(def mid-x (quot max-x 2))

(def max-y (quot height cell-size))
(def mid-y (quot max-y 2))


(defn scaled [min max value]
  (+ min (* value (- max min))))


(defn dist [x y]
  (js/Math.abs (- x y)))


(defn cell-opacity [x y]
  (let [n-base (q/noise (* 0.1 x) (* 0.1 y))
        n-noise (q/noise (* 0.8 x) (* 0.8 y) 2000)

        v-base (scaled -1.5 2.2 n-base)
        v-noise (scaled -0.3 0.3 n-noise)]
    (+ v-base v-noise)))


(defn cell-neighbors [cx cy]
  (->>
    [[0 -1] [-1 0] [1 0] [0 1]]
    (map (fn [[xo yo]] [(+ cx xo) (+ cy yo)]))
    (filter (fn [[x y]] (and (<= 0 x)
                             (< x max-x)
                             (<= 0 y)
                             (< y max-y))))))

(defn find-visible-cells
  ([] (find-visible-cells #{[mid-x mid-y]} (list [mid-x mid-y]) #{} 0))
  ([seen frontier visible visible-count]
    (if (or (= 0 (count frontier))
            (<= max-blob-size visible-count))
      visible
      (let [cell (first frontier)
            visible? (< 0.20 (apply cell-opacity cell))
            neighbors (apply cell-neighbors cell)
            unseen (filter (comp not (partial contains? seen)) neighbors)
            
            new-seen (s/union seen (set unseen))
            new-frontier (if visible?
                           (concat (rest frontier) unseen)
                           (rest frontier))
            new-visible (if visible?
                          (conj visible cell)
                          visible)
            new-visible-count (+ visible-count (if visible? 1 0))]
        (recur new-seen new-frontier new-visible new-visible-count)))))

(def visible-cells (atom #{}))

(defn setup []
  (q/frame-rate 4)
  (q/color-mode :hsb 360 1.0 1.0 1.0)
  (reset! visible-cells (find-visible-cells))
  {})


(defn update-state [state]
  state)


(defn draw-state [state]
  ; Clear the sketch by filling it with light-grey color.
  (q/background 0 0)
  (q/no-stroke)

  (doseq [x (range (quot width cell-size))
          y (range (quot height cell-size))]
    (let [opacity (cell-opacity x y)
          color (if (contains? @visible-cells [x y])
                  lavender-color
                  background-color)]
      (apply q/fill (conj color opacity))
      (apply q/rect (mapv #(* % cell-size)
                          [x y 1 1])))))


(q/defsketch pixel-blobs
  :host "pixel-blobs"
  :size [width height]
  ; setup function called only once, during sketch initialization.
  :setup setup
  ; update-state is called on each iteration before draw-state.
  :update update-state
  :draw draw-state
  ; This sketch uses functional-mode middleware.
  ; Check quil wiki for more info about middlewares and particularly
  ; fun-mode.
  :middleware [m/fun-mode])