FROM eclipse-temurin:17-jdk

# 필요한 도구 설치
RUN apt-get update && apt-get install -y wget gnupg2 unzip curl && \
    apt-get clean

# Google Chrome 설치
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb

# Chromedriver 설치
RUN wget -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/123.0.6312.122/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip chromedriver -d /usr/local/bin/ && \
    rm /tmp/chromedriver.zip

# 환경변수 설정
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver


VOLUME /tmp
#ARG JAR_FILE = ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]