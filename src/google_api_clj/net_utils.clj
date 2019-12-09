(ns google-api-clj.net-utils
  (:require
   [clojure.java.io       :as io]))

(def max-attempts 4) ;; Overall the request should take less than 30 seconds
(def grace-period 1000)

(defn execute [request]
  ;; exponential backoff according to https://developers.google.com/drive/api/v3/handle-errors#exponential-backoff
  (loop [attempt 0]
    (let [[status result] (try
                            [:ok (.execute request)]
                            (catch Throwable ex
                              (if (< attempt max-attempts)
                                [:retry ex]
                                [:error ex])))]
      (condp = status
        :ok    result
        :error (throw result)
        :retry (do
                 (Thread/sleep (* (Math/pow 2 attempt) grace-period))
                 (recur (inc attempt)))))))

(defn execute-media-and-download-to [request path]
  (loop [attempt 1]
    (let [[status result] (try
                            [:ok (with-open [out (io/output-stream path)]
                                   (.executeMediaAndDownloadTo request out))]
                            (catch Throwable ex
                              (if (<= attempt max-attempts)
                                [:retry ex]
                                [:error ex])))]
      (condp = status
        :ok    result
        :error (throw result)
        :retry (do
                 #_(log/warn "Failed to execute request" attempt result)
                 (Thread/sleep (* attempt grace-period))
                 (recur (inc attempt)))))))
