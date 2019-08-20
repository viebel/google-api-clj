(ns google-api-clj.net-utils
  (:require
   [clojure.java.io       :as io]))

(def max-attempts 3)
(def grace-period 300)

(defn execute [request]
  (loop [attempt 1]
    (let [[status result] (try
                            [:ok (.execute request)]
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
