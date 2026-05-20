FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates ca-certificates-java \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY . .
RUN mvn -B clean verify \
    && cp target/mvn2llm.jar /build/mvn2llm.jar \
    && jdeps \
      --ignore-missing-deps \
      --recursive \
      --multi-release 21 \
      --print-module-deps \
      /build/mvn2llm.jar \
      > /build/modules.txt \
    && jlink \
      --add-modules "$(cat /build/modules.txt)" \
      --strip-debug \
      --compress=2 \
      --no-header-files \
      --no-man-pages \
      --output /opt/jre \
    && mkdir -p /work/tmp

FROM gcr.io/distroless/base-debian13:nonroot
COPY --from=build --chown=nonroot:nonroot /work /work
COPY --from=build /opt/jre /jre
COPY --from=build /build/mvn2llm.jar /app/mvn2llm.jar
WORKDIR /work
ENTRYPOINT ["/jre/bin/java","-XX:MaxRAMPercentage=75","-Djava.io.tmpdir=/work/tmp","-jar","/app/mvn2llm.jar"]
