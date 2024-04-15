FROM eclipse-temurin:17-jdk-alpine
# apk를 사용하여 필요한 패키지 설치
RUN apk update \
    && apk add --no-cache chromium chromium-chromedriver curl unzip \
    && rm -rf /var/cache/apk/*

VOLUME /tmp
#ARG JAR_FILE = ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]