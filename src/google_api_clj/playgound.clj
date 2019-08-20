(ns google-api-clj.playgound
  (:require [google-api-clj.google-client :refer [create-google-client]]
            [clojure.reflect]
            [google-api-clj.drive-service :refer [make-service]]
            [google-api-clj.sheets-service :refer [auto-resize-rows]]))


(comment

  (def credential-path "/Users/viebel/.config/gcloud/application_default_credentials.json")
  (def google-client (create-google-client credential-path))
  (def service (make-service google-client))

  (auto-resize-rows {:service service}
                    "10ZuwK3uTJ_dFm34LqoFJewtrwrpmqz2luyoGjBC1vm0"
                    "aa"
                    1
                    10)

  (def r (clojure.reflect/reflect service))
  (map :name (:members r))
  ;; => (revisions replies DEFAULT_ROOT_URL DEFAULT_BATCH_PATH teamdrives com.google.api.services.drive.Drive permissions channels files about com.google.api.services.drive.Drive comments DEFAULT_SERVICE_PATH changes DEFAULT_BASE_URL initialize)
  (clojure.pprint/print-table (:members r))
  )
