(ns google-api-clj.playgound
  (:require clojure.reflect
            [google-api-clj.drive-service :as drive]
            [google-api-clj.google-client :refer [create-google-client]]
            [google-api-clj.sheets-service :as sheets])
  (:import com.google.api.services.drive.model.Change
           com.google.api.services.sheets.v4.model.ValueRange))



(comment

  (def credential-path "/Users/viebel/.config/gcloud/application_default_credentials.json")
  (def google-client (create-google-client credential-path))
  (def sheets-service (sheets/make-service google-client))
  (def drive-service (drive/make-service google-client))

  (def my-spreadheet (sheets/create-spreadsheet
                      {:service sheets-service}
                      {:spreadsheet-properties/title "created automagically by Stas lib"}
                      [{:sheet-properties/title "Summaries"}
                       {:sheet-properties/title "Data"
                        :sheet-properties/grid
                        {:grid-properties/columns 4
                         :grid-properties/rows 8
                         ;; => 8      :grid-properties/frozen-columns 1
                         :grid-properties/frozen-rows    1}}]))
  (:spreadsheet/id my-spreadheet)
  ;; => "1Cy8FlY4VPrFiitrd-eu6492lphkK9Matp7tyGCtqGpM";; => "1k9-ZDAZSLBDKSLS_nUAvAz69L44WMdF1XzJJ6GNhABU"  ;; => "1Q4BOdey6UhM5Cw6O926vCO92Dkq-L8qwH-akXknIlc8"
  (:spreadsheet/url my-spreadheet)

  (drive/share-file {:service drive-service} (:spreadsheet/id my-spreadheet) #_"stask312@gmail.com" "viebel@gmail.com")
  (def r (clojure.reflect/reflect (-> sheets-service .spreadsheets)))
  (map :name (:members r))
  (-> drive-service .changes (.getStartPageToken) .execute)
  ;; => {"kind" "drive#startPageToken", "startPageToken" "23"}


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

  (-> sheets-service
      .spreadsheets
      .values
      (.update (:spreadsheet/id my-spreadheet)
               "A2"
               (-> (ValueRange.)
                   (.setValues
                    (vector-2d->ArrayList [[1e6 10] [9 8] [4 5]]))))
      (.setValueInputOption  "USER_ENTERED")
      .execute)

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

  ;; => (revisions replies DEFAULT_ROOT_URL DEFAULT_BATCH_PATH teamdrives com.google.api.services.drive.Drive permissions channels files about com.google.api.services.drive.Drive comments DEFAULT_SERVICE_PATH changes DEFAULT_BASE_URL initialize)
  (clojure.pprint/print-table (:members r)))
