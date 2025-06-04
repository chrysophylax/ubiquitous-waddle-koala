#! /bin/bash
curl -v http://localhost:8085/stations
curl -v http://localhost:8085/stations/valfritt-id
curl -v http://localhost:8085/stations/valfritt-id?type=1&from=2025-05-01&to=2025-06-04
curl -v http://localhost:8085/types
