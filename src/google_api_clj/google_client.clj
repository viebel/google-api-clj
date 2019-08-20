(ns google-api-clj.google-client)

(ns google-api-clj.google-client
  (:require
   [clojure.java.io            :as io])
  (:import
   (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
   (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
   (com.google.api.client.json.jackson2 JacksonFactory)
   (com.google.api.services.sheets.v4 SheetsScopes)
   (com.google.api.services.drive DriveScopes)
   (com.google.api.client.http HttpRequestInitializer)))

;; ===========================================================================
;; utils

(defn make-credential [path]
  (with-open [in (io/input-stream path)]
    (-> (GoogleCredential/fromStream in)
        (.createScoped [SheetsScopes/SPREADSHEETS
                        DriveScopes/DRIVE]))))

(defn make-timeout-fixer [initializer timeout]
  (proxy [HttpRequestInitializer] []
    (initialize [http-request]
      (.initialize initializer http-request)
      (.setConnectTimeout http-request timeout)
      (.setReadTimeout http-request timeout))))

;; ===========================================================================
;; component

#_(defn start [component]
    (assoc component
           :http-transport (GoogleNetHttpTransport/newTrustedTransport)
           :json-factory   (JacksonFactory/getDefaultInstance)
           :credential     (make-timeout-fixer (make-credential credential-path)
                                               (* 3 60000))))

#_(defn stop [component]
    (dissoc component :http-transport :json-factory :credential))

;; ===========================================================================
;; constructor

(defn new-google-client [config]
  (map->GoogleClient (select-keys config [:credential-path :application-name])))

(comment

  (def credential-path "/Users/viebel/.config/gcloud/application_default_credentials.json")
  (def google-client  {:http-transport (GoogleNetHttpTransport/newTrustedTransport)
                       :json-factory   (JacksonFactory/getDefaultInstance)
                       :application-name "Mr Hankey"
                       :credential     (make-timeout-fixer (make-credential credential-path)
                                                           (* 3 60000))})

  )
