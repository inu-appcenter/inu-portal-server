FROM openjdk:17.0.1-jdk-slim

# 패키지 목록 업데이트 및 필요한 패키지 설치
RUN apt-get -y update && \
    apt-get -y install wget unzip curl && \
    wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt -y install ./google-chrome-stable_current_amd64.deb

# ChromeDriver 설치
RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/$(curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE)/chromedriver_linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/bin && \
    rm /tmp/chromedriver.zip

# ChromeDriver 실행 권한 부여
RUN chmod +x /usr/bin/chromedriver


VOLUME /tmp
#ARG JAR_FILE = ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]