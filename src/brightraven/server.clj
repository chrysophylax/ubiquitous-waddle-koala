(ns brightraven.server
  (:require [ring.util.response :as resp]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [compojure.core :refer :all]	
            [cheshire.core :refer :all]
	    [clojure.tools.logging :refer :all]
            [compojure.route :as route]
            [compojure.middleware :as midl]
            [brightraven.sql :as bsql]
            [brightraven.smhi :as bs]
            [try-let :refer [try-let]])
  (:gen-class))

(def json-pprinter
  (create-pretty-printer
   (assoc default-pretty-print-options
          :indent-arrays? true)))

(defn format-json-reply [db-data]
 (resp/content-type  (resp/response
   (generate-string db-data {:pretty json-pprinter})) "application/json"))

(defn handle-station
  [req]
  (log :info ["Received request:" req])
   (if (empty? (:query-params req))
    (format-json-reply (bsql/get-station (get-in req [:route-params :station])))
    (try-let [from (java.time.LocalDate/parse (get-in req [:query-params "from"]))
              to (java.time.LocalDate/parse (get-in req [:query-params "to"]))
              type (get-in req [:query-params "type"])
              station (get-in req [:route-params :station])]
             (format-json-reply (bsql/get-average from to station type))
             (catch java.lang.NullPointerException npe
               (log :error ["missing params for station average calculation, exception:\n" npe])
               (resp/bad-request "Please supply type, from, to parameters")))))

;; route declarations
(defroutes app-routes
  (GET "/favicon.ico" [] (resp/response ""))
  (GET "/types" [] (format-json-reply (bsql/get-params)))
  (GET "/stations/:station" request (handle-station request))
  (GET "/stations" [] (format-json-reply (bsql/get-stations)))
  (route/not-found "Not Found"))

;; main app-handler
(defn handler [request]
  (routing request app-routes))

(def app
  (-> handler
      (midl/wrap-canonical-redirect)
      (params/wrap-params)
      ))

(def http-conf {:join? false :port (Integer/valueOf (or (System/getenv "port") "8085"))})

(def station-amount
  (let [max-sites (System/getenv "max")]
  (if (nil? max-sites)
    nil
    (Integer/valueOf max-sites))))
(defn -main
  [& args]
  (log :info "Starting HTTP server")
  (jetty/run-jetty app http-conf)
  (log :info "Loading SMHI data...")
  (doall (bs/load-smhi-data station-amount))
  (log :info (str "Loading step done. You can now query the database and more! http://localhost:" (:port http-conf) "/stations")))
