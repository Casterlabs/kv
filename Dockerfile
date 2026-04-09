FROM eclipse-temurin:24-jre-ubi9-minimal
WORKDIR /home/container

LABEL org.opencontainers.image.source="https://github.com/casterlabs/kv"

COPY ./target/kv.jar /home/container

ENV KV_PORT=8080
ENV KV_ROOT=/data

CMD [ "java", "-XX:+CrashOnOutOfMemoryError", "-jar", "kv.jar" ]
EXPOSE 8080/tcp