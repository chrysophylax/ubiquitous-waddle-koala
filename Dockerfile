FROM clojure:temurin-24-tools-deps-alpine
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY deps.edn /usr/src/app/
RUN clj -P
COPY . /usr/src/app
CMD ["clj", "-X", "brightraven.server/-main"]
