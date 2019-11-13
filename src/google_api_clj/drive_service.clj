(ns google-api-clj.drive-service
  (:require [clojure.java.io :as io])
  (:import
   (com.google.api.client.http FileContent)
   (com.google.api.services.drive Drive$Builder)
   (com.google.api.client.googleapis.json GoogleJsonResponseException)
   (com.google.api.services.drive.model Permission Channel File)))


;; ===========================================================================
;; utils

(def default-page-size (int 1000))
(def default-fields "nextPageToken, files(id, name, mimeType, createdTime, modifiedTime)")

(defn make-service [{:keys [http-transport json-factory credential application-name]}]
  (-> (Drive$Builder. http-transport json-factory credential)
      (.setApplicationName application-name)
      .build))

(defn list-files-request [service mime-type page-token]
  (cond-> (-> service .files .list (.setPageSize default-page-size) (.setFields default-fields))
    mime-type  (.setQ (format "mimeType='%s'" mime-type))
    page-token (.setPageToken page-token)))

;; ===========================================================================
;; API
(def mime-types
  {:pdf            "application/pdf"
   :google-excel   "application/vnd.google-apps.spreadsheet"
   :ms-excel       "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
   :oo-spreadsheet "application/x-vnd.oasis.opendocument.spreadsheet"
   :csv            "text/csv"
   :folder         "application/vnd.google-apps.folder"
   :html           "application/zip"})

#_(defn list-files [{:keys [service]} kind]
    (loop [files [] page-token nil]
      (let [result     (execute (list-files-request service (get mime-types kind) page-token))
            files      (into files
                             (map (fn [file]
                                    {:id       (.getId file)
                                     :name     (.getName file)
                                     :type     (.getMimeType file)
                                     :created  (some-> (.getCreatedTime file) .toStringRfc3339)
                                     :modified (some-> (.getModifiedTime file) .toStringRfc3339)})
                                  (.getFiles result)))
            page-token (.getNextPageToken result)]
        (if (string/blank? page-token)
          files
          (recur files page-token)))))

(defn create-folder [{:keys [service]} name]
  (let [file-metadata (-> (File.)
                          (.setName name)
                          (.setMimeType (:folder mime-types)))]
    (-> service .files (.create file-metadata) .execute)))

(defn move-file [{:keys [service]} id dest-folder-id]
  (let [parent (-> service
                   .files
                   (.get id)
                   (.setFields "parents")
                   .execute
                   (get "parents")
                   first)]
    (-> service
        .files
        (.update id nil)
        (.setAddParents dest-folder-id)
        (.setRemoveParents parent)
        .execute)))

(defn delete-file [{:keys [service]} id]
  (-> service .files (.delete id) .execute))

(defn get-file [{:keys [service]} id]
  (-> service .files (.get id)
      (.setFields "*")
      .execute))

(defn file-exists? [{:keys [service]} id]
  (try
    (-> service .files (.get id)
        (.setFields "id")
        .execute)
    true
    (catch GoogleJsonResponseException e
      (if (clojure.string/starts-with? (ex-message e) "404")
        false
        (throw e)))))

#_(defn export-file [{:keys [service]} id format path]
    (let [request (-> service .files (.export id (get mime-types format)))]
      (execute-media-and-download-to request path)))

(defn share-file [{:keys [service]} id user]
  (-> service
      .permissions
      (.create id
               (-> (Permission.)
                   (.setType "user")
                   (.setRole "writer")
                   (.setEmailAddress user)))
      .execute))

(defn quotas [{:keys [service]}]
  (let [about (-> service .about .get (.setFields "*") .execute)]
    {:user  (let [user (.getUser about)]
              {:id    (.getPermissionId user)
               :email (.getEmailAddress user)
               :name  (.getDisplayName user)})
     :quota (let [quota (.getStorageQuota about)]
              {:limit                (.getLimit quota)
               :usage                (.getUsage quota)
               :usage-in-drive       (.getUsageInDrive quota)
               :usage-in-drive-trash (.getUsageInDriveTrash quota)})}))

(defn upload-file [{:keys [service]} file-path file-name mime-type]
  (let [file-metadata (->
                       (File.)
                       (.setName file-name))
        content (FileContent. mime-type (io/file file-path))]
    (-> service .files
        (.create file-metadata content)
        (.setFields "id")
        .execute)))



(comment

  (def service google-api-clj.playground/drive-service )

  (defn upload-in-folder-and-share [{:keys [service]} {:keys [file-path file-name mime-type destination-folder-id user]}]
    (let [file (upload-file {:service service} file-path file-name mime-type)]
      (move-file {:service service} (get file "id") destination-folder-id)
      (share-file {:service service} (get file "id") user)))

  (upload-file {:service service} "/tmp/aaa.csv" "bb.csv" "text/csv")
  (upload-in-folder-and-share {:service service} {:file-path "/tmp/aaa.csv"
                                                  :file-name "bb.csv"
                                                  :mime-type "text/csv"
                                                  :destination-folder-id "1LU2RfkXyXFJDe5HgKTjxNfdYba2XuMAB"
                                                  :user "viebel@gmail.com"})
  (def file-metadata
    (->
     (File.)
     (.setName "aaa.csv")))
  (def content (FileContent. "text/csv" (clojure.java.io/file "/tmp/aaa.csv")))
  (def file (-> service .files
                (.create file-metadata content)
                (.setFields "*")
                .execute
                ))
  (move-file {:service service} (get file "id") "1LU2RfkXyXFJDe5HgKTjxNfdYba2XuMAB")

  (file-exists? {:service service} "1SmGqJFZzhn1wRgZsbLG8NHyAvT-5ybhM")
  (share-file  {:service service} "1SmGqJFZzhn1wRgZsbLG8NHyAvT-5ybhM" "viebel@gmail.com")

  (file-exists? {:service service} "1cdcROEdhd3BMvOebgE0oTPUFNByPNGucXoRl4rGz3n8" #_(get file "id"))
  )
