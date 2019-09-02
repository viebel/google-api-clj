(ns google-api-clj.drive-service
  (:import
   (com.google.api.services.drive Drive$Builder)
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

