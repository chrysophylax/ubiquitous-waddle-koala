(ns brightraven.smhi
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [brightraven.sql :as bsql]))

;; generic helper
(defn try-n-times
  "Normand's decoupled retrier"
  [f n]
      (if (zero? n)
        (f)
        (try
          (f)
          (catch Throwable _
            (try-n-times f (dec n))))))

(defn fetch-json "fetch-and-parse json" [s]
 (json/read-str (try-n-times #(slurp s) 2)))


(defn parse-epoch [l]
  (when-not (nil? l) 
    (java.time.Instant/ofEpochMilli l)))


(defn get-json-link
  "Always look for the JSON link when walking"
  [resource]
  (let [links (resource "link")]
    ((first (filter #(= (% "type") "application/json") links)) "href")))

(defn build-csv-link
  "we cheat by skipping the last link walk lookup and just building the csv uri
  by hand"
  [latest-months-url]
  (clojure.string/replace latest-months-url #"\.json" "/data.json"))

(defn get-last-months
  "finds the latest-months url"
  [station]
  (let [periods (station "period")
        latest-months (filter #(= (% "key") "latest-months") periods)]
    (if-not (empty? latest-months)
      (-> (first latest-months)
          (get-json-link)
          (build-csv-link)))))
(defn format-row
  "complect values into our row format"
  [vals meta]
  (merge meta {:date (parse-epoch (get vals "date"))
   :value (edn/read-string (get vals "value")) ; coerce to a clojure number
   :quality (get vals "quality")}))
(defn build-resource [e]
  (let [id (e "key")
        name (e "title")
        link (get-json-link e)]
    {:id id :name name :link link}))
(defn get-stations
  "fetch stations with latest-months data"
  [r]
  (let [param (fetch-json (:link r))
        station-set (get param "station")
        station-data-links (pmap (comp get-last-months fetch-json get-json-link) station-set)
        filtered (remove nil? station-data-links)]
    filtered))
(defn format-station-data
  "format a station x param json doc to a map"
  [url]
  (let [json-doc (fetch-json url)
        station-values (get json-doc "value")
        station-meta (get json-doc "station")
        station-param (get json-doc "parameter")
        last-updated (get json-doc "updated")
        meta {:station-id (station-meta "key")
              :station-name (station-meta "name")
              :param-id (station-param "key")
              :param-name (station-param "name")
              :meta-last-updated (parse-epoch last-updated)}]
        (print ".")
        (into [] (map #(format-row % meta) station-values))))

(defn cond-take [n c]
  (if-not (nil? n)
    (take n c)
    c))
(defn load-smhi-data
  "loads all smhi data"
  [count]
  (let [base-doc (fetch-json "https://opendata-download-metobs.smhi.se/api/version/latest.json")
        resources (base-doc "resource")]
    (as-> resources r
      (map build-resource r)
      (sort-by :id r)
      (map get-stations r)
      (flatten r)
      (cond-take count r)
      (pmap format-station-data r)
      (map (comp last bsql/insert-rows!) r))))
