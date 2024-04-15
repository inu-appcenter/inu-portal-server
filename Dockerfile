FROM eclipse-temurin:17-jdk


# 크롬 브라우저 설치
RUN apt-get -y update

RUN apt -y install wget

RUN apt -y install unzip

RUN apt -y install curl

RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb

RUN apt -y install ./google-chrome-stable_current_amd64.deb


# 크롬 드라이버 설치
RUN wget https://storage.googleapis.com/chrome-for-testing-public/123.0.6312.122/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip -d /usr/bin/ && \
    rm chromedriver-linux64.zip

# 환경변수 PATH에 크롬 드라이버 경로 추가
ENV PATH="/usr/bin/chromedriver-linux64:${PATH}"

VOLUME /tmp
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]