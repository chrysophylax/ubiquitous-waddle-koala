(ns brightraven.sql
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :refer :all]
            ))

(def db-uri "jdbc:sqlite:file:memdb1?mode=memory")

(def conn (j/get-connection {:connection-uri db-uri}))

(def db {:connection conn })
(def map-entities {:entities #(.replace % \- \_)}) ; sql standard does not allow '-' in unquoted col identifiers

                                        ;(j/execute! db "drop table observations")
(j/execute! db "create table if not exists observations (id integer primary key autoincrement, param_id integer, param_name text, station_id integer, station_name text, meta_last_updated timestamp, value TEXT, quality TEXT, date timestamp)" )

(defn insert-rows! [rows]
  (j/insert-multi! db :observations rows map-entities))

(defn get-stations []
  (j/query db ["select distinct station_name as station, station_id as id, param_id as type_id, param_name as type from observations GROUP BY station_id, param_id;"]))

(defn get-station [station]
  (let [resp (j/query db "select station_name as station, station_id as id, param_name as type, param_id as type_id, date as measurement from observations")]
  (log :info ["DB result: " resp])
  resp))

(defn get-params []
  (j/query db ["select distinct param_name as type, param_id as type_id from observations order by param_id;"]))

(defn get-average
  "given a start, end, a station and a parameter name, return the average" 
  [start end station param]
  (let [resp (j/query db
                      ["select station_name as station, station_id as id, param_name as type, param_id as type_id, avg(value) as average_measurement FROM observations WHERE date between ? AND ? AND station_id = ? and param_id = ? GROUP BY station_id, param_id" start end station param])
        ]
    (log :info ["DB result: " resp])
    resp))
