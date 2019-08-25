(defproject viebel/google-api-clj "0.1.3"
  :description "Google API Clojure driver"
  :url "https://github.com/viebel/google-api-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.google.api-client/google-api-client           "1.28.0"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.28.0"]
                 [com.google.apis/google-api-services-sheets        "v4-rev20190109-1.28.0"]
                 [com.google.apis/google-api-services-drive         "v3-rev20181213-1.28.0"]])
