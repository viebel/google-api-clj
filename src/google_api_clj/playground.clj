(ns google-api-clj.playground
  (:require clojure.reflect
            [clojure.spec.test.alpha :as stest]
            [google-api-clj.drive-service :as drive]
            [google-api-clj.google-client :refer [create-google-client]]
            [google-api-clj.sheets-service :as sheets])
  (:import com.google.api.services.drive.model.Change
           com.google.api.services.sheets.v4.model.ValueRange))



(defn bar [a b {:keys [x y] :as args}]
  (sc.api/spy)
  (def a a)
  (* a y (+ x b))
  a)

(defn foo [a]
  (sc.api/spy)
  (bar a 9 {:x 89 :y (inc 890)}))



(defn headers-and-rows->maps
  "Receives a tabular collection where the first elememt contains the headers
  and the rest of the elements are the rows.
  Returns a collection where each row is converted into a map whose keys are the headers.
  In rows whose number of elements is lower than the number of headers, the missing headers won't appear in the corresponding map.
  In rows whose number of elements is higher than the number of headers, the additional elements won't appear in the corresponding map.

  See also: headers-and-maps->rows.

  ~~~klipse
  (headers-and-rows->maps  [\"name\" \"title\" \"total\"]
                           [[\"David\" \"Architect\" 19]
                            [\"Anna\" \"Dev\"]
                            [\"Joe\" \"Analyst\" 88 321]])
  ~~~
  "
  [headers rows]
  (map (partial zipmap headers) rows))
(defn vec->map
  "Converts a 2d vec to a hash-map.

  ~~~klipse
   (vec->map [[:a 1] [:b 2]])
  ~~~
   "
  [vec]
  (into {} vec))

(defn map-2d-vec
  "Maps the values of a `2D` vector where each element of the vector is a key-value pair.
  `f` is a `1-ary` function that receives the key.

  ~~~klipse
  (map-2d-vec inc [[:a 1] [:b 2]])
  ~~~
  "
  [f m]
  (map (fn[[k id]] [k (f id)]) m))

(defn map-2d-vec-kv
  "Maps the values of a `2D` vector where each element of the vector is a key-value pair.
  `fk` is a `1-ary` function that receives the key.
  `fv` is a `1-ary` function that receives the value.

  ~~~klipse
    (map-2d-vec-kv name inc [[:a 1] [:b 2]])
  ~~~
  "
  [fk fv m]
  (map (fn[[k id]] [(fk k) (fv id)]) m))

(defn map-object
  "Returns a map with the same keys as `m` and with the values transformed by `f`. `f` is a `1-ary` function that receives the key.

  ~~~klipse
  (map-object inc {:a 1 :b 2 :c 3})
  ~~~
  "
  [f m]
  (vec->map (map-2d-vec f m)))

(comment
  (stest/instrument)

  (def credential-path "/Users/viebel/.config/gcloud/application_default_credentials.json")
  (def google-client (create-google-client {:credential-path credential-path
                                            :scopes [:drive :spreadsheets]
                                            :application-name "Playground"}))
  (def sheets-service (sheets/make-service google-client))
  (def drive-service (drive/make-service google-client))

  (def my-spreadheet (sheets/create-spreadsheet
                      {:service sheets-service}
                      {:spreadsheet-properties/title "Part of a shared folder - Auto shared"}
                      []))
  (:spreadsheet/id my-spreadheet)
  ;; => "1Cy8FlY4VPrFiitrd-eu6492lphkK9Matp7tyGCtqGpM";; => "1k9-ZDAZSLBDKSLS_nUAvAz69L44WMdF1XzJJ6GNhABU"  ;; => "1Q4BOdey6UhM5Cw6O926vCO92Dkq-L8qwH-akXknIlc8"
  (:spreadsheet/url my-spreadheet)
  ;; => "https://docs.google.com/spreadsheets/d/1HOh7B2FAiRgvFdsUJBE5EY_Sv0hEhIHlCP2J4NarsLE/edit"  ;; => "https://docs.google.com/spreadsheets/d/10vHR7reZ8ZTBSjeXIO-7oK3iIivw8KS4RmpLaJ4cNko/edit";; => "https://docs.google.com/spreadsheets/d/1QmSlR1Q9XN3sQv5CJxR8135kkoCaMlAoX313_riPHyY/edit"  ;; => "https://docs.google.com/spreadsheets/d/12xHwm0hrCGNSUo7XjgQGbh5z9gRGK0eEjHZJSUWT4rE/edit"
  (drive/share-file {:service drive-service} (:spreadsheet/id my-spreadheet) #_"stask312@gmail.com"  "viebel@gmail.com")
  (def file (drive/get-file {:service drive-service} (:spreadsheet/id my-spreadheet)))
  (def root (drive/get-file {:service drive-service} "root"))
  (def folder (drive/create-folder {:service drive-service} "Mr Hankey"))
  (drive/share-file {:service drive-service} (get folder "id")  "viebel@gmail.com")
  (get file "parents")
  (def r (clojure.reflect/reflect (-> sheets-service .spreadsheets  )))
  (map :name (:members r))
  (first (:members r))

  (drive/move-file {:service drive-service} (:spreadsheet/id my-spreadheet) (get folder "id"))
  (-> drive-service
      .files
      (.get (:spreadsheet/id my-spreadheet))
      (.setFields "parents")
      .execute
      (get "parents")
      first)


  (def rows (sheets/get-rows {:service sheets-service} "1HOh7B2FAiRgvFdsUJBE5EY_Sv0hEhIHlCP2J4NarsLE"
                             :range "schema"))


  (defn clean-schema [schema]
    (map #(dissoc % "table") schema))
  (->> (let [[header & rows] (sheets/rows->values rows)]
         (headers-and-rows->maps header rows))
       (group-by #(get % "table"))
       (map-object clean-schema)
       )
  (-> drive-service
      .files
      (.watch
       (:spreadsheet/id my-spreadheet)
       (-> (Channel.)
           #_(.setResourceId "23")
           (.setId (:spreadsheet/id my-spreadheet))
           (.setAddress "https://tranquil-hollows-11766.herokuapp.com/google-drive-change")
           (.setType "web_hook")
           (.setPayload true)
           (.setToken "target=myapp")))
      .execute)

  (def ans (sheets/add-sheet {:service sheets-service} (:spreadsheet/id my-spreadheet) {:sheet-properties/title "NewData4"}))
  (-> (.getReplies ans)
      (.get 0)
      .getAddSheet
      .getProperties
      .getSheetId
      )
  (.getAddSheet (first (into [] )))
  (:spreadsheet/url my-spreadheet)
  (dotimes [i 100]
    (println i)
    (time (println (sheets/update-rows {:service sheets-service} (:spreadsheet/id my-spreadheet)
                                       (repeat 1e4 [8882.5 "cnn.com" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"]) :range (str "Sheet1!A" (inc (int (* i 1e4))))))))

  (dotimes [i 100]
    (println i)
    (time (println (sheets/append-rows {:service sheets-service} (:spreadsheet/id my-spreadheet)
                                       (repeat 1e4 [8882.5 "cnn.com" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"]) :range (str "Sheet1!A" (inc (int (* i 1e4))))))))

  (time (do
          (def rows (sheets/get-rows {:service sheets-service} (:spreadsheet/id my-spreadheet)
                                     :range "Sheet1"))
          (def ddd (sheets/rows->values rows))
          (count ddd)))

  (def revisions (-> drive-service
                     .revisions
                     (.list "1k9-ZDAZSLBDKSLS_nUAvAz69L44WMdF1XzJJ6GNhABU" #_"1Cy8FlY4VPrFiitrd-eu6492lphkK9Matp7tyGCtqGpM")
                     #_(.get "1Cy8FlY4VPrFiitrd-eu6492lphkK9Matp7tyGCtqGpM" "3")
                     (.setFields "*")
                     .execute))
  (type revisions)
  (-> (last (get revisions "revisions"))
      (get "id"))
  (-> drive-service
      .changes
      )

  (def changes (-> drive-service
                   .changes
                   (.list "63")
                   (.setRestrictToMyDrive true)
                   #_(.setDriveId)
                   .execute))

  (count (map  #(get-in % ["file" "id"])
               (.getChanges changes)))

  (->  (.getChanges changes)
       first)
  (.getNewStartPageToken changes)
  (.getNextPageToken changes)

  (clojure.pprint/print-table (:members r)))
