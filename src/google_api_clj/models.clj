(ns google-api-clj.models
  (:require
   [clojure.spec.alpha :as s])
  (:import
   (com.google.api.services.sheets.v4.model GridProperties
                                            SheetProperties
                                            Sheet
                                            SpreadsheetProperties
                                            Spreadsheet
                                            GridCoordinate
                                            OverlayPosition
                                            EmbeddedObjectPosition
                                            GridRange
                                            ValueRange
                                            ChartData
                                            ChartSourceRange
                                            PieChartSpec
                                            ChartSpec
                                            BooleanCondition
                                            ConditionValue
                                            DataValidationRule
                                            ExtendedValue
                                            CellFormat
                                            TextFormat
                                            NumberFormat
                                            Color
                                            CellData
                                            RowData
                                            EmbeddedChart
                                            DimensionRange
                                            DimensionProperties
                                            Borders
                                            Border
                                            TextRotation
                                            Padding)))


(comment
  (stest/instrument))

;; The idea here to use Clojure spec to document the types
;; Ideally, it would automatically conform the types, so there will be no need for manual casting to int. Will do it one day.


;; GridCoordinate
(s/def :grid-coordinate/sheet-id nat-int?)
(s/def :grid-coordinate/row-index nat-int?)
(s/def :grid-coordinate/column-index nat-int?)
(s/def ::grid-coordinate (s/keys :req [:grid-coordinate/sheet-id
                                       :grid-coordinate/row-index
                                       :grid-coordinate/column-index]))

(defn grid-coordinate [coordinate]
  (let [{:keys [:grid-coordinate/sheet-id
                :grid-coordinate/row-index
                :grid-coordinate/column-index]} (s/assert ::grid-coordinate coordinate)]
    (-> (GridCoordinate.)
        (.setSheetId (int sheet-id))
        (.setRowIndex (int row-index))
        (.setColumnIndex (int column-index)))))

(s/fdef grid-coordinate
  :args (s/cat :coordinate ::grid-coordinate)
  :ret (partial instance? GridCoordinate))

;; OverlayPosition
(s/def :overlay-position/anchor-cell ::grid-coordinate)
(s/def :overlay-position/width-pixels nat-int?)
(s/def :overlay-position/height-pixels nat-int?)
(s/def ::overlay-position (s/keys :req [:overlay-position/anchor-cell
                                        :overlay-position/width-pixels
                                        :overlay-position/height-pixels]))

(defn overlay-position [position]
  (let [{:keys [:overlay-position/anchor-cell
                :overlay-position/width-pixels
                :overlay-position/height-pixels]} (s/assert ::overlay-position position)]
    (-> (OverlayPosition.)
        (.setAnchorCell (grid-coordinate anchor-cell))
        (.setWidthPixels (int width-pixels))
        (.setHeightPixels (int height-pixels)))))

(s/fdef overlay-position
  :args (s/cat :position ::overlay-position)
  :ret (partial instance? OverlayPosition))

;; EmbeddedObjectPosition
(s/def :embedded-object-position/overlay ::overlay-position)
(s/def ::embedded-object-position (s/keys :req [:embedded-object-position/overlay]))

(defn embedded-object-position [position]
  (let [{:keys [:embedded-object-position/overlay]} (s/assert ::embedded-object-position position)]
    (-> (EmbeddedObjectPosition.)
        (.setOverlayPosition (overlay-position overlay)))))

(s/fdef embedded-object-position
  :args (s/cat :position ::embedded-object-position)
  :ret (partial instance? EmbeddedObjectPosition))

;; GridRange

(s/def :grid-range/sheet-id nat-int?)
(s/def :grid-range/start-row nat-int?)
(s/def :grid-range/start-column nat-int?)
(s/def :grid-range/end-row nat-int?)
(s/def :grid-range/end-column nat-int?)
(s/def ::grid-range (s/keys :req [:grid-range/sheet-id
                                  :grid-range/start-row
                                  :grid-range/start-column
                                  :grid-range/end-row
                                  :grid-range/end-column]))

(defn grid-range [range]
  (let [{:keys [:grid-range/sheet-id
                :grid-range/start-row
                :grid-range/start-column
                :grid-range/end-row
                :grid-range/end-column]} (s/assert ::grid-range range)]
    (-> (GridRange.)
        (.setSheetId (int sheet-id))
        (.setStartRowIndex (int start-row))
        (.setStartColumnIndex (int start-column))
        (.setEndRowIndex (int end-row))
        (.setEndColumnIndex (int end-column)))))

(s/fdef grid-range
  :args (s/cat :range ::grid-range)
  :ret (partial instance? GridRange))

;; ChartData
(s/def :chart-data/source-range ::grid-range)
(s/def ::chart-data (s/keys :req [:chart-data/source-range]))

(defn chart-data [data]
  (let [{:keys [:chart-data/source-range]} (s/assert ::chart-data data)]
    (-> (ChartData.)
        (.setSourceRange (-> (ChartSourceRange.)
                             (.setSources [(grid-range source-range)]))))))

;; PieChartSpec

(s/def :pie-chart-spec/legend-position #{"LABELED_LEGEND" "LEFT_LEGEND" "RIGHT_LEGEND" "BOTTOM_LEGEND"})
(s/def :pie-chart-spec/domain ::chart-data)
(s/def :pie-chart-spec/series ::chart-data)
(s/def :pie-chart-spec/three-dimensional? boolean?)
(s/def ::pie-chart-spec (s/keys :req [:pie-chart-spec/legend-position
                                      :pie-chart-spec/domain
                                      :pie-chart-spec/series
                                      :pie-chart-spec/three-dimensional?]))

(defn pie-chart-spec [spec]
  (let [{:keys [:pie-chart-spec/legend-position
                :pie-chart-spec/domain
                :pie-chart-spec/series
                :pie-chart-spec/three-dimensional?]} (s/assert ::pie-chart-spec spec)]
    (-> (PieChartSpec.)
        (.setLegendPosition legend-position)
        (.setDomain (chart-data domain))
        (.setSeries (chart-data series))
        (.setThreeDimensional three-dimensional?))))

(s/fdef pie-chart-spec
  :args (s/cat :spec ::pie-chart-spec)
  :ret (partial instance? PieChartSpec))

;; ChartSpec
(s/def :chart-spec/title string?)
(s/def :chart-spec/pie-chart ::pie-chart-spec)
(s/def ::chart-spec (s/keys :req [:chart-spec/title
                                  (or :chart-spec/pie-chart)]))

(defn chart-spec [spec]
  (let [{:keys [:chart-spec/title
                :chart-spec/pie-chart]} (s/assert ::chart-spec spec)]
    (cond-> (-> (ChartSpec.)
                (.setTitle title))
      pie-chart (.setPieChart (pie-chart-spec pie-chart)))))

(s/fdef chart-spec
  :args (s/cat :spec ::chart-spec)
  :ret (partial instance? ChartSpec))

;; EmbeddedChart

(s/def :embedded-chart/spec ::chart-spec)
(s/def :embedded-chart/position ::embedded-object-position)
(s/def ::embedded-chart (s/keys :req [:embedded-chart/spec
                                      :embedded-chart/position]))

(defn embedded-chart [chart]
  (let [{:keys [:embedded-chart/spec :embedded-chart/position]} (s/assert ::embedded-chart chart)]
    (-> (EmbeddedChart.)
        (.setSpec (chart-spec spec))
        (.setPosition (embedded-object-position position)))))

(s/fdef embedded-chart
  :args (s/cat :chart ::embedded-chart)
  :ret (partial instance? EmbeddedChart))

;; GridProperties
(s/def :grid-properties/columns nat-int?)
(s/def :grid-properties/rows nat-int?)
(s/def :grid-properties/frozen-columns nat-int?)
(s/def :grid-properties/frozen-rows nat-int?)
(s/def :grid-properties/hide-gridlines? boolean?)
(s/def ::grid-properties (s/keys :opt [:grid-properties/columns
                                       :grid-properties/rows
                                       :grid-properties/frozen-columns
                                       :grid-properties/frozen-rows
                                       :grid-properties/hide-gridlines?]))

(defn grid-properties [properties]
  (let [{:keys [:grid-properties/columns
                :grid-properties/rows
                :grid-properties/frozen-columns
                :grid-properties/frozen-rows
                :grid-properties/hide-gridlines?]} (s/assert ::grid-properties properties)]
    (cond-> (GridProperties.)
      columns         (.setColumnCount (int columns))
      rows            (.setRowCount (int rows))
      frozen-columns  (.setFrozenColumnCount (int frozen-columns))
      frozen-rows     (.setFrozenRowCount (int frozen-rows))
      hide-gridlines? (.setHideGridlines true))))

(s/fdef grid-properties
  :args (s/cat :properties ::grid-properties)
  :ret (partial instance? GridProperties))

;; ExtendedValue
(s/def :extended-value/string string?)
(s/def :extended-value/number double?)
(s/def :extended-value/formula string?)
(s/def ::extended-value (s/keys :req [(or :extended-value/string
                                          :extended-value/number
                                          :extended-value/formula)]))

(defn extended-value [value]
  (let [{:keys [:extended-value/string
                :extended-value/number
                :extended-value/formula]} (s/assert ::extended-value value)]
    (cond-> (ExtendedValue.)
      string  (.setStringValue string)
      number  (.setNumberValue number)
      formula (.setFormulaValue formula))))

(s/fdef extended-value
  :args (s/cat :value ::extended-value)
  :ret (partial instance? ExtendedValue))

;; Color

(s/def :color/red float?)
(s/def :color/green float?)
(s/def :color/blue float?)
(s/def :color/alpha float?)
(s/def ::color (s/keys :req [:color/red :color/green :color/blue]
                       :opt [:color/alpha]))

(defn color [properties]
  (let [{:keys [:color/red :color/green :color/blue :color/alpha]} (s/assert ::color properties)]
    (cond-> (-> (Color.)
                (.setRed (float red))
                (.setGreen (float green))
                (.setBlue (float blue)))
      alpha (.setAlpha (float alpha)))))

(s/fdef color
  :args (s/cat :properties ::color)
  :ret (partial instance? Color))

;; TextFormat
(s/def :text-format/bold? boolean?)
(s/def :text-format/italic? boolean?)
(s/def :text-format/strikethrough? boolean?)
(s/def :text-format/underline? boolean?)
(s/def :text-format/font-size pos-int?)
(s/def :text-format/font-family string?)
(s/def :text-format/foreground-color ::color)
(s/def ::text-format (s/keys :opt [:text-format/bold?
                                   :text-format/italic?
                                   :text-format/strikethrough?
                                   :text-format/underline?
                                   :text-format/font-size
                                   :text-format/font-family
                                   :text-format/foreground-color]))

(defn text-format [format]
  (let [{:keys [:text-format/bold?
                :text-format/italic?
                :text-format/strikethrough?
                :text-format/underline?
                :text-format/font-size
                :text-format/font-family
                :text-format/foreground-color]} (s/assert ::text-format format)]
    (cond-> (TextFormat.)
      bold?            (.setBold true)
      italic?          (.setItalic true)
      strikethrough?   (.setStrikethrough true)
      underline?       (.setUnderline true)
      font-size        (.setFontSize (int font-size))
      font-family      (.setFontFamily font-family)
      foreground-color (.setForegroundColor (color foreground-color)))))

(s/fdef text-format
  :args (s/cat :format ::text-format)
  :ret (partial instance? TextFormat))

;; NumberFormat
(s/def :number-format/type #{"TEXT" "NUMBER" "PERCENT" "CURRENCY" "DATE" "TIME" "DATE_TIME" "SCIENTIFIC"})
(s/def :number-format/pattern string?)
(s/def ::number-format (s/keys :req [:number-format/type :number-format/pattern]))

(defn number-format [format]
  (let [{:keys [:number-format/type :number-format/pattern]} (s/assert ::number-format format)]
    (-> (NumberFormat.)
        (.setType type)
        (.setPattern pattern))))

(s/fdef number-format
  :args (s/cat :format ::number-format)
  :ret (partial instance? NumberFormat))

;; Border
(s/def :border/color ::color)
(s/def :border/style #{"DOTTED" "DASHED" "SOLID" "SOLID_MEDIUM" "SOLID_THICK" "DOUBLE"})
(s/def :border/width nat-int?)
(s/def ::border (s/keys :req [:border/color :border/style :border/width]))

(defn border [border]
  (let [v (s/assert ::border border)]
    (-> (Border.)
        (.setColor (color (:border/color v)))
        (.setStyle (:border/style v))
        (.setWidth (int (:border/width v))))))

(s/fdef border
  :args (s/cat :border ::border)
  :ret (partial instance? Border))

;; Borders
(s/def :borders/top ::border)
(s/def :borders/left ::border)
(s/def :borders/right ::border)
(s/def :borders/bottom ::border)
(s/def ::borders (s/keys :opt [:borders/top :borders/left :borders/right :borders/bottom]))

(defn borders [borders]
  (let [v (s/assert ::borders borders)]
    (cond-> (Borders.)
      (:borders/top v) (.setTop (border (:borders/top v)))
      (:borders/left v) (.setLeft (border (:borders/left v)))
      (:borders/right v) (.setRight (border (:borders/right v)))
      (:borders/bottom v) (.setBottom (border (:borders/bottom v))))))

(s/fdef borders
  :args (s/cat :borders ::borders)
  :ret (partial instance? Borders))

;; TextRotation
(s/def :text-rotation/angle nat-int?)
(s/def :text-rotation/vertical? boolean?)
(s/def ::text-rotation (s/keys :opt [:text-rotation/angle :text-rotation/vertical?]))

(defn text-rotation [text-rotation]
  (let [v (s/assert ::text-rotation text-rotation)]
    (cond-> (TextRotation.)
      (:text-rotation/angle v)     (.setAngle (int (:text-rotation/angle v)))
      (:text-rotation/vertical? v) (.setVertical (:text-rotation/vertical? v)))))

(s/fdef text-rotation
  :args (s/cat :text-rotation ::text-rotation)
  :ret (partial instance? TextRotation))

;; Padding
(s/def :padding/top nat-int?)
(s/def :padding/left nat-int?)
(s/def :padding/bottom nat-int?)
(s/def :padding/right nat-int?)
(s/def ::padding (s/keys :opt [:padding/top :padding/left :padding/right :padding/bottom]))

(defn padding [padding]
  (let [v (s/assert ::padding padding)]
    (cond-> (Padding.)
      (:padding/top v) (.setTop (int (:padding/top v)))
      (:padding/left v) (.setLeft (int (:padding/left v)))
      (:padding/right v) (.setRight (int (:padding/right v)))
      (:padding/bottom v) (.setBottom (int (:padding/bottom v))))))

(s/fdef padding
  :args (s/cat :padding ::padding)
  :ret (partial instance? Padding))

;; CellFormat
(s/def :cell-format/horizontal-alignment #{"LEFT" "CENTER" "RIGHT" "JUSTIFY"})
(s/def :cell-format/vertical-alignment #{"TOP" "MIDDLE" "BOTTOM"})
(s/def :cell-format/wrap-strategy #{"WRAP" "CLIP"})
(s/def :cell-format/background-color ::color)
(s/def :cell-format/text-format ::text-format)
(s/def :cell-format/number-format ::number-format)
(s/def :cell-format/borders ::borders)
(s/def :cell-format/rotation ::text-rotation)
(s/def ::cell-format (s/keys :opt [:cell-format/text-format
                                   :cell-format/number-format
                                   :cell-format/horizontal-alignment
                                   :cell-format/vertical-alignment
                                   :cell-format/background-color
                                   :cell-format/wrap-strategy
                                   :cell-format/borders
                                   :cell-format/text-rotation
                                   :cell-format/padding]))

(defn cell-format [format]
  (let [v (s/assert ::cell-format format)]
    (cond-> (CellFormat.)
      (:cell-format/horizontal-alignment v) (.setHorizontalAlignment (:cell-format/horizontal-alignment v))
      (:cell-format/vertical-alignment v)   (.setVerticalAlignment (:cell-format/vertical-alignment v))
      (:cell-format/wrap-strategy v)        (.setWrapStrategy (:cell-format/wrap-strategy v))
      (:cell-format/background-color v)     (.setBackgroundColor (color (:cell-format/background-color v)))
      (:cell-format/text-format v)          (.setTextFormat (text-format (:cell-format/text-format v)))
      (:cell-format/number-format v)        (.setNumberFormat (number-format (:cell-format/number-format v)))
      (:cell-format/borders v)              (.setBorders (borders (:cell-format/borders v)))
      (:cell-format/rotation v)             (.setTextRotation (text-rotation (:cell-format/rotation v)))
      (:cell-format/padding v)              (.setPadding (padding (:cell-format/padding v))))))

(s/fdef cell-format
  :args (s/cat :format ::cell-format)
  :ret (partial instance? CellFormat))

;; BooleanCondition

(s/def :boolean-condition/type #{"ONE_OF_LIST"})
(s/def :boolean-condition/values (s/coll-of string?))
(s/def ::boolean-condition (s/keys :req [:boolean-condition/type
                                         :boolean-condition/values]))

(defn boolean-condition [condition]
  (let [{:keys [:boolean-condition/type
                :boolean-condition/values]} (s/assert ::boolean-condition condition)]
    (-> (BooleanCondition.)
        (.setType type)
        (.setValues (map #(.setUserEnteredValue (ConditionValue.) %) values)))))

(s/fdef boolean-condition
  :args (s/cat :condition ::boolean-condition)
  :ret (partial instance? BooleanCondition))

;; DataValidationRule

(s/def :data-validation-rule/condition ::boolean-condition)
(s/def :data-validation-rule/strict? boolean?)
(s/def :data-validation-rule/show-custom-ui? boolean?)
(s/def ::data-validation-rule (s/keys :req [:data-validation-rule/condition
                                            :data-validation-rule/strict?
                                            :data-validation-rule/show-custom-ui?]))

(defn data-validation-rule [rule]
  (let [{:keys [:data-validation-rule/condition
                :data-validation-rule/strict?
                :data-validation-rule/show-custom-ui?]} (s/assert ::data-validation-rule rule)]
    (-> (DataValidationRule.)
        (.setCondition (boolean-condition condition))
        (.setStrict strict?)
        (.setShowCustomUi show-custom-ui?))))

(s/fdef data-validation-rule
  :args (s/cat :rule ::data-validation-rule)
  :ret (partial instance? DataValidationRule))

;; CellData

(s/def :cell-data/value ::extended-value)
(s/def :cell-data/format ::cell-format)
(s/def :cell-data/link string?)
(s/def :cell-data/data-validation ::data-validation-rule)
(s/def ::cell-data (s/keys :req [(or :cell-data/value :cell-data/link)]
                           :opt [:cell-data/format
                                 :cell-data/data-validation]))

(defn cell-data [data]
  (let [{:keys [:cell-data/value
                :cell-data/format
                :cell-data/link
                :cell-data/data-validation]} (s/assert ::cell-data data)]
    (cond-> (-> (CellData.)
                (.setUserEnteredValue (extended-value value)))
      format          (.setUserEnteredFormat (cell-format format))
      link            (.setHyperlink link)
      data-validation (.setDataValidation (data-validation-rule data-validation)))))

(s/fdef cell-data
  :args (s/cat :data ::cell-data)
  :ret (partial instance? CellData))

;; RowData
(s/def :row-data/values (s/coll-of ::cell-data))
(s/def ::row-data (s/keys :req [:row-data/values]))

(defn row-data [data]
  (let [{:keys [:row-data/values]} (s/assert ::row-data data)]
    (-> (RowData.)
        (.setValues (map cell-data values)))))

(s/fdef row-data
  :args (s/cat :data ::row-data)
  :ret (partial instance? RowData))

;; SheetProperties
(s/def :sheet-properties/id nat-int?)
(s/def :sheet-properties/title string?)
(s/def :sheet-properties/grid ::grid-properties)
(s/def ::sheet-properties (s/keys :opt [:sheet-properties/id
                                        :sheet-properties/title
                                        :sheet-properties/grid]))

(defn sheet-properties [properties]
  (let [{:keys [:sheet-properties/id
                :sheet-properties/title
                :sheet-properties/grid]} (s/assert ::sheet-properties properties)]
    (cond-> (SheetProperties.)
      id    (.setSheetId (int id))
      title (.setTitle title)
      grid  (.setGridProperties (grid-properties grid)))))

(s/fdef sheet-properties
  :args (s/cat :properties ::sheet-properties)
  :ret (partial instance? SheetProperties))

;; Sheet
(defn sheet [properties]
  (-> (Sheet.)
      (.setProperties (sheet-properties properties))))

(s/fdef sheet
  :args (s/cat :properties ::sheet-properties)
  :ret (partial instance? Sheet))

;;; convert back to normal form

(defn translate-grid-properties [^GridProperties properties]
  {:grid-properties/rows           (.getRowCount properties)
   :grid-properties/columns        (.getColumnCount properties)
   :grid-properties/frozen-rows    (.getFrozenRowCount properties)
   :grid-properties/frozen-columns (.getFrozenColumnCount properties)})

(defn translate-sheet-properties [^SheetProperties properties]
  {:sheet-properties/id    (.getSheetId properties)
   :sheet-properties/type  (.getSheetType properties)
   :sheet-properties/title (.getTitle properties)
   :sheet-properties/index (.getIndex properties)
   :sheet-properties/grid  (translate-grid-properties (.getGridProperties properties))})

(defn translate-spreadsheet-properties [^SpreadsheetProperties properties]
  {:spreadsheet-properties/title (.getTitle properties)})

;; SpreadsheetProperties
(s/def :spreadsheet-properties/title string?)
(s/def ::spreadsheet-properties (s/keys :req [:spreadsheet-properties/title]))

(defn spreadsheet-properties [properties]
  (let [{:keys [:spreadsheet-properties/title]} (s/assert ::spreadsheet-properties properties)]
    (-> (SpreadsheetProperties.)
        (.setTitle title))))

(s/fdef spreadsheet-properties
  :args (s/cat :properties ::spreadsheet-properties)
  :ret (partial instance? SpreadsheetProperties))

;; Spreadsheet
(defn spreadsheet [properties sheets]
  (-> (Spreadsheet.)
      (.setProperties (spreadsheet-properties properties))
      (.setSheets (map sheet sheets))))

(s/fdef spreadsheet
  :args (s/cat :properties ::spreadsheet-properties
               :sheets (s/coll-of ::sheet-properties))
  :ret (partial instance? Spreadsheet))

;; DimensionRange
(s/def :dimension-range/sheet-id nat-int?)
(s/def :dimension-range/dimension #{"ROWS" "COLUMNS"})
(s/def :dimension-range/start-index nat-int?)
(s/def :dimension-range/end-index nat-int?)
(s/def ::dimension-range (s/keys :req [:dimension-range/sheet-id
                                       :dimension-range/dimension
                                       :dimension-range/start-index
                                       :dimension-range/end-index]))

(defn dimension-range [data]
  (let [v (s/assert ::dimension-range data)]
    (cond-> (DimensionRange.)
      :always (.setSheetId (int (:dimension-range/sheet-id v)))
      :always (.setDimension (:dimension-range/dimension v))
      :always (.setStartIndex (int (:dimension-range/start-index v)))
      (:dimension-range/end-index v) (.setEndIndex (int (:dimension-range/end-index v))))))

(s/fdef dimension-range
  :args (s/cat :data ::dimension-range)
  :ret (partial instance? DimensionRange))

;; DimensionProperties
(s/def :dimension-properties/pixel-size nat-int?)
(s/def :dimension-properties/hidden-by-filter? boolean?)
(s/def :dimension-properties/hidden-by-user? boolean?)

(s/def ::dimension-properties (s/keys :opt [:dimension-properties/pixel-size
                                            :dimension-properties/hidden-by-filter?
                                            :dimension-properties/hidden-by-user?]))

(defn dimension-properties [data]
  (let [v (s/assert ::dimension-properties data)]
    (cond-> (DimensionProperties.)
      (:dimension-properties/pixel-size v)        (.setPixelSize (int (:dimension-properties/pixel-size v)))
      (:dimension-properties/hidden-by-filter? v) (.setHiddenByFilter (boolean (:dimension-properties/hidden-by-filter? v)))
      (:dimension-properties/hidden-by-user? v)   (.setHiddenByUser (boolean (:dimension-properties/hidden-by-user? v))))))

(s/fdef dimension-properties
  :args (s/cat :data ::dimension-properties)
  :ret (partial instance? DimensionProperties))
