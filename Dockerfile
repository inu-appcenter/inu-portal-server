FROM eclipse-temurin:17-jdk
VOLUME /tmp
ENV PATH="/usr/bin:${PATH}"
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*
#ARG JAR_FILE = ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]