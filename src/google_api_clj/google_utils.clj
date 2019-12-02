(ns google-api-clj.google-utils)

(defn error-code [exception]
  (-> exception
      .getDetails
      (get "code")))
