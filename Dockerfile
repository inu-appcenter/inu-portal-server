FROM openjdk:17.0.1-jdk-slim

RUN apt-get -y update && \
    apt-get -y install wget unzip curl libglib2.0-0 libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdbus-1-3 libxkbcommon0 libxdamage1 libxcomposite1 libxrandr2 libgbm1 libpango-1.0-0 libcairo2 libasound2 libatspi2.0-0 libgtk-3-0

# Chrome zip 파일 다운로드
RUN wget https://storage.googleapis.com/chrome-for-testing-public/114.0.5735.133/linux64/chrome-linux64.zip

# Zip 파일 압축 해제
RUN unzip chrome-linux64.zip -d /opt/chrome

# 압축 해제된 실행 파일에 실행 권한 부여
RUN chmod +x /opt/chrome/chrome-linux/chrome

# 환경변수 설정 (선택적)
ENV PATH="/opt/chrome/chrome-linux:${PATH}"

# ChromeDriver 설치
RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/bin && \
    rm /tmp/chromedriver.zip

# ChromeDriver 실행 권한 부여
RUN chmod +x /usr/bin/chromedriver


VOLUME /tmp
#ARG JAR_FILE = ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]