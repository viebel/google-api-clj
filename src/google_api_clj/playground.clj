(ns google-api-clj.playground
  (:require clojure.reflect
            [clojure.spec.test.alpha :as stest]
            [google-api-clj.drive-service :as drive]
            [google-api-clj.google-client :refer [create-google-client]]
            [google-api-clj.sheets-service :as sheets])
  (:import com.google.api.services.drive.model.Change
           com.google.api.services.sheets.v4.model.ValueRange))



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
  ;; => "https://docs.google.com/spreadsheets/d/12xHwm0hrCGNSUo7XjgQGbh5z9gRGK0eEjHZJSUWT4rE/edit"
  (drive/share-file {:service drive-service} (:spreadsheet/id my-spreadheet) #_"stask312@gmail.com" "viebel@gmail.com")
  (def file (drive/get-file {:service drive-service} (:spreadsheet/id my-spreadheet)))
  (def root (drive/get-file {:service drive-service} "root"))
  (def folder (drive/create-folder {:service drive-service} "Mr Hankey"))
  (drive/share-file {:service drive-service} (get folder "id")  "viebel@gmail.com")
  (get file "parents")
  (def r (clojure.reflect/reflect (-> sheets-service .spreadsheets  .get)))
  (map :name (:members r))
  (first (:members r))
;; => (getByDataFilter
;;     com.google.api.services.sheets.v4.Sheets$Spreadsheets
;;     create
;;     get
;;     batchUpdate
;;     developerMetadata
;;     values
;;     this$0
;;     sheets)  (-> drive-service .changes (.getStartPageToken) .execute)
  ;; => {"kind" "drive#startPageToken", "startPageToken" "23"}


  (drive/move-file {:service drive-service} (:spreadsheet/id my-spreadheet) (get folder "id"))
  (-> drive-service
      .files
      (.get (:spreadsheet/id my-spreadheet))
      (.setFields "parents")
      .execute
      (get "parents")
      first)




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
