(ns google-api-clj.sheets-service
  (:require
   [clojure.string             :as string]
   [google-api-clj.net-utils      :refer [execute]]
   [google-api-clj.models  :as m])
  (:import
   [com.google.api.services.sheets.v4.model
    AddChartRequest
    AddSheetRequest
    BatchUpdateSpreadsheetRequest
    DeleteSheetRequest
    DeleteRangeRequest
    EmbeddedChart
    MergeCellsRequest
    Request
    UpdateCellsRequest
    UpdateBordersRequest
    UpdateSheetPropertiesRequest
    AutoResizeDimensionsRequest
    UpdateDimensionPropertiesRequest
    RepeatCellRequest]
   com.google.api.services.sheets.v4.Sheets$Builder))

;; Javadoc for the Google Sheets API used here:
;; https://developers.google.com/resources/api-libraries/documentation/sheets/v4/java/latest/

;; ===========================================================================
;; constants

(def header-bgcolor {:color/red 0.788235294
                     :color/green 0.854901961
                     :color/blue 0.968627451})

(def border-color {:color/red 0
                   :color/green 0
                   :color/blue 1})

(def dotted-border {:border/color border-color
                    :border/style "DOTTED"
                    :border/width 1})

(def dotted-borders {:borders/top dotted-border
                     :borders/left dotted-border
                     :borders/right dotted-border
                     :borders/bottom dotted-border})

(def outer-border-color {:color/red   0.117647059
                         :color/green 0.509803922
                         :color/blue  0.57254902})

(def infobox-text-color {:color/red   0.062745098
                         :color/green 0.325490196
                         :color/blue  0.57254902})

(def solid-outer-border {:border/color outer-border-color
                         :border/style "SOLID"
                         :border/width 1})
(def solid-borders {:borders/top    solid-outer-border
                    :borders/left   solid-outer-border
                    :borders/right  solid-outer-border
                    :borders/bottom solid-outer-border})
(def infobox-borders {:borders/bottom solid-outer-border})

(def title-bgcolor1 {:color/red   0.062745098
                     :color/green 0.325490196
                     :color/blue  0.57254902})

(def title-bgcolor2 {:color/red   0.254901961
                     :color/green 0.6
                     :color/blue  0.819607843})

(def title-bgcolor3 {:color/red   0.788235294
                     :color/green 0.854901961
                     :color/blue  0.964705882})

(def white-color {:color/red   1
                  :color/green 1
                  :color/blue  1})
(def black-color {:color/red   0
                  :color/green 0
                  :color/blue  0})
(def light-gray-color {:color/red   0.921568627
                       :color/green 0.921568627
                       :color/blue  0.921568627})

;; status colors (converted from QtestRun::BOOTSTRAP_STATUS_MAPPER_COLORS)
(def run-status-colors
  {:passed        {:color/red   0.274509804
                   :color/green 0.533333333
                   :color/blue  0.278431373}
   :failed        {:color/red   0.725490196
                   :color/green 0.290196078
                   :color/blue  0.282352941}
   :not_completed {:color/red   0.290196078
                   :color/green 0.482352941
                   :color/blue  0.549019608}
   :no_run        {:color/red   0.466666667
                   :color/green 0.466666667
                   :color/blue  0.466666667}
   :blocked       {:color/red   0.752941176
                   :color/green 0.596078431
                   :color/blue  0.325490196}
   :n_a           {:color/red   0.22745098
                   :color/green 0.529411765
                   :color/blue  0.674509804}})

(def max-runs 10)


;; XXX Accedo colors
;;     We'll move it to report metadata once the accedo report is ready


(def accedo-header-bg {:color/red   0.796078431
                       :color/green 0.266666667
                       :color/blue  0.180392157})
(def accedo-odd-bg {:color/red   0.850980392
                    :color/green 0.850980392
                    :color/blue  0.850980392})
(def accedo-even-bg {:color/red   0.811764706
                     :color/green 0.88627451
                     :color/blue  0.949019608})

(def ^:dynamic tabular-bg white-color)

;; ===========================================================================
;; color utils

(defn hex-color-component->double [hex-component]
  (double (/ (Long/parseLong hex-component 16) 255)))

(defn hex-color->color [hex]
  (if-let [[_ hr hg hb] (re-find #"^#([\da-fA-F]{2})([\da-fA-F]{2})([\da-fA-F]{2})$" hex)]
    {:color/red   (hex-color-component->double hr)
     :color/green (hex-color-component->double hg)
     :color/blue  (hex-color-component->double hb)}
    white-color))

;; ===========================================================================
;; Sheets model wrappers

(defn make-service [{:keys [http-transport json-factory credential application-name]}]
  (-> (Sheets$Builder. http-transport json-factory credential)
      (.setApplicationName application-name)
      .build))

(defn make-embedded-pie-chart [sheet-id row-offset chart-column]
  (let [start-row (inc row-offset)
        end-row   (+ row-offset (count (:available_values chart-column)) 2)]
    (m/embedded-chart
     {:embedded-chart/spec
      {:chart-spec/title (str (:name chart-column) " Summary")
       :chart-spec/pie-chart
       {:pie-chart-spec/legend-position    "LABELED_LEGEND"
        :pie-chart-spec/domain             {:chart-data/source-range {:grid-range/sheet-id     sheet-id
                                                                      :grid-range/start-row    start-row
                                                                      :grid-range/start-column 0
                                                                      :grid-range/end-row      end-row
                                                                      :grid-range/end-column   1}}
        :pie-chart-spec/series             {:chart-data/source-range {:grid-range/sheet-id     sheet-id
                                                                      :grid-range/start-row    start-row
                                                                      :grid-range/start-column 1
                                                                      :grid-range/end-row      end-row
                                                                      :grid-range/end-column   2}}
        :pie-chart-spec/three-dimensional? true}}
      :embedded-chart/position
      {:embedded-object-position/overlay
       {:overlay-position/anchor-cell   {:grid-coordinate/sheet-id     sheet-id
                                         :grid-coordinate/row-index    (inc row-offset)
                                         :grid-coordinate/column-index 3}
        :overlay-position/width-pixels  600
        :overlay-position/height-pixels 450}}})))

;; ===========================================================================
;; Sheets requests

(defn make-update-cells [start rows fields]
  (-> (Request.)
      (.setUpdateCells (-> (UpdateCellsRequest.)
                           (.setStart start)
                           (.setRows rows)
                           (.setFields fields)))))

(defn make-update-range-cells [range rows fields]
  (-> (Request.)
      (.setUpdateCells (-> (UpdateCellsRequest.)
                           (.setRange range)
                           (.setRows rows)
                           (.setFields fields)))))

(defn make-set-background [range color]
  (-> (Request.)
      (.setRepeatCell (-> (RepeatCellRequest.)
                          (.setRange (m/grid-range range))
                          (.setCell (m/cell-data {:cell-data/format {:cell-format/background-color color}}))
                          (.setFields "userEnteredFormat(backgroundColor)")))))

(defn make-update-borders [range {:keys [left top right bottom]}]
  (-> (Request.)
      (.setUpdateBorders (cond-> (UpdateBordersRequest.)
                           :always (.setRange (m/grid-range range))
                           left    (.setLeft (m/border left))
                           top     (.setTop (m/border top))
                           right   (.setRight (m/border right))
                           bottom  (.setBottom (m/border bottom))))))

(defn make-merge-cells [sheet-id start-row start-column end-row end-column]
  (-> (Request.)
      (.setMergeCells (-> (MergeCellsRequest.)
                          (.setMergeType "MERGE_ALL")
                          (.setRange (m/grid-range {:grid-range/sheet-id     sheet-id
                                                    :grid-range/start-row    start-row
                                                    :grid-range/start-column start-column
                                                    :grid-range/end-row      end-row
                                                    :grid-range/end-column   end-column}))))))

(defn make-resize-sheet [sheet-id rows]
  (-> (Request.)
      (.setUpdateSheetProperties
       (-> (UpdateSheetPropertiesRequest.)
           (.setProperties (m/sheet-properties {:sheet-properties/id   sheet-id
                                                :sheet-properties/grid {:grid-properties/rows rows}}))
           (.setFields "gridProperties(rowCount)")))))

(defn make-summary-pie-chart [sheet-id row-offset chart-column]
  (let [chart (make-embedded-pie-chart sheet-id row-offset chart-column)]
    (-> (Request.)
        (.setAddChart (-> (AddChartRequest.)
                          (.setChart chart))))))

(defn make-add-sheet [properties]
  (-> (Request.)
      (.setAddSheet (-> (AddSheetRequest.)
                        (.setProperties (m/sheet-properties properties))))))

(defn make-delete-sheet [id]
  (-> (Request.)
      (.setDeleteSheet (-> (DeleteSheetRequest.)
                           (.setSheetId (int id))))))

(defn make-delete-range [sheet-id start-row start-column end-row end-column]
  (-> (Request.)
      (.setDeleteRange (-> (DeleteRangeRequest.)
                           (.setRange (m/grid-range {:grid-range/sheet-id     sheet-id
                                                     :grid-range/start-row    start-row
                                                     :grid-range/start-column start-column
                                                     :grid-range/end-row      end-row
                                                     :grid-range/end-column   end-column}))
                           (.setShiftDimension "ROWS")))))

(defn make-batch-update [requests]
  (-> (BatchUpdateSpreadsheetRequest.)
      (.setRequests requests)))

(defn make-auto-resize-dimensions [sheet-id dimension start-index end-index]
  (-> (Request.)
      (.setAutoResizeDimensions
       (-> (AutoResizeDimensionsRequest.)
           (.setDimensions (m/dimension-range
                            {:dimension-range/sheet-id    sheet-id
                             :dimension-range/dimension   dimension
                             :dimension-range/start-index start-index
                             :dimension-range/end-index   end-index}))))))

(defn make-update-dimension-properties [sheet-id dimension start-index end-index pixel-size]
  (-> (Request.)
      (.setUpdateDimensionProperties
       (-> (UpdateDimensionPropertiesRequest.)
           (.setRange (m/dimension-range
                       {:dimension-range/sheet-id sheet-id
                        :dimension-range/dimension dimension
                        :dimension-range/start-index start-index
                        :dimension-range/end-index end-index}))
           (.setProperties (m/dimension-properties
                            {:dimension-properties/pixel-size pixel-size}))
           (.setFields "pixelSize")))))

;; ===========================================================================
;; formulas

(defn index->identifier [index]
  (let [ab "ABCDEFGHIJKLMNOPQRSTUVWXYZ"]
    (string/join
     (loop [identifier [] index index]
       (if (< index 26)
         (cons (nth ab index) identifier)
         (recur (cons (nth ab (rem index 26)) identifier) (dec (quot index 26))))))))

(defn make-countif-formula* [column-index value-index start end]
  (let [column-identifier (index->identifier column-index)]
    (str "=COUNTIF(Data!" column-identifier start ":" column-identifier end ", A" value-index ")")))

(defn make-countif-formula [column-index value-index entities-count]
  (make-countif-formula* column-index value-index 2 (inc entities-count)))

(defn make-count-blank-formula* [column-index start end]
  (let [column-identifier (index->identifier column-index)]
    (str "=COUNTBLANK(Data!" column-identifier start ":" column-identifier end ")")))

(defn make-count-blank-formula [column-index entities-count]
  (make-count-blank-formula* column-index 2 (inc entities-count)))

(defn make-conditional-count-blank-formula [column-index step-column-index start end]
  (let [column-identifier      (index->identifier column-index)
        step-column-identifier (index->identifier step-column-index)]
    (str "=COUNTIFS(Data!" column-identifier start ":" column-identifier end ",\"\","
         "Data!" step-column-identifier start ":" step-column-identifier end ",\"\")"
         "+COUNTIFS(Data!" column-identifier start ":" column-identifier end ",\"\","
         "Data!" step-column-identifier start ":" step-column-identifier end ",\"1\")")))

;; ===========================================================================
;; value transformations

(defn safe-truncate* [{:keys [max-length prefix suffix] :or {prefix "TRUNCATED: "}} s]
  (if (and s (> (count s) max-length))
    (do
      #_(log/warnf "Truncating string to %d" max-length)
      (str prefix (subs s 0 max-length) suffix))
    s))

(defn safe-truncate [s max-length]
  (safe-truncate* {:max-length max-length} s))

(defn encode-url-title [s]
  (string/escape (safe-truncate* {:max-length 240 :prefix "" :suffix "..."} s)
                 {\" "'"}))

(defn url-formula-value
  ([url] (format "=HYPERLINK(\"%s\")" url))
  ([url title] (format "=HYPERLINK(\"%s\", \"%s\")"
                       url
                       (encode-url-title title))))

;;; bunch of PT-specific code redacted, functions below left as example.

(defn create-spreadsheet [{:keys [service]} properties sheets]
  (let [spreadsheet (-> service
                        .spreadsheets
                        (.create (m/spreadsheet properties sheets))
                        execute)]
    {:spreadsheet/id         (.getSpreadsheetId spreadsheet)
     :spreadsheet/properties (m/translate-spreadsheet-properties (.getProperties spreadsheet))
     :spreadsheet/sheets     (map #(m/translate-sheet-properties (.getProperties %)) (.getSheets spreadsheet))}))


(defn auto-resize-rows [{:keys [service]} id sheet-id start-row end-row]
  (-> service
      .spreadsheets
      (.batchUpdate id (make-batch-update [(make-auto-resize-dimensions sheet-id "ROWS" start-row end-row)]))
      execute
      .getSpreadsheetId))

;; ===========================================================================
;; component

#_(defrecord SheetsService [google-client]

    component/Lifecycle

    (start [component]
      #_(log/info ";; starting SheetsService")
      (assoc component :service (make-service google-client)))

    (stop [component]
      #_(log/info ";; stopping SheetsService")
      (dissoc component :service)))

;; ===========================================================================
;; constructor

#_(defn new-sheets-service [config]
    (component/using
     (map->SheetsService (select-keys config []))
     [:google-client]))

(comment
  (auto-resize-rows {:service google-api-clj.drive-service/service}
                    "10ZuwK3uTJ_dFm34LqoFJewtrwrpmqz2luyoGjBC1vm0"
                    "aa"
                    "A"
                    "M")
  )
