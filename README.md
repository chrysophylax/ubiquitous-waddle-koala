# smhi

## Kör med clojure lokalt installerat
```
$ port=9090 max=1 clojure -X brightraven.server/-main
```

## Kör med docker
```
$ docker build . -t smhi
$ docker run -p 9090 -e max=5 smhi
```
## Tips
1. Sätt inte max variabeln för att hämta ner hela SMHIs databank (tar några minuter)

## tankar
Crawlern kan optimeras lite med async go channels, just nu är den ganska dum och itererar igenom alla JSON dokument för att hitta latest-months url. En mer aggressiv crawler skulle kunna bara generera upp potentiella urls, distribuera till workers och skrapa.


Kika i sample.sh för exempel på anrop som API:t stödjer.
Enkel kod.
