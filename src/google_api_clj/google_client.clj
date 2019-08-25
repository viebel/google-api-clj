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

(def scope-map
  {:spreadsheets SheetsScopes/SPREADSHEETS
   :drive        DriveScopes/DRIVE})

(defn make-credential [path scopes]
  (with-open [in (io/input-stream path)]
    (-> (GoogleCredential/fromStream in)
        (.createScoped (map scope-map scopes)))))

(defn make-timeout-fixer [initializer timeout]
  (proxy [HttpRequestInitializer] []
    (initialize [http-request]
      (.initialize initializer http-request)
      (.setConnectTimeout http-request timeout)
      (.setReadTimeout http-request timeout))))

(defn create-google-client [{:keys [credential-path scopes application-name]}]
  {:http-transport (GoogleNetHttpTransport/newTrustedTransport)
   :json-factory   (JacksonFactory/getDefaultInstance)
   :application-name application-name
   :credential     (make-timeout-fixer (make-credential credential-path scopes)
                                       (* 3 60000))})
