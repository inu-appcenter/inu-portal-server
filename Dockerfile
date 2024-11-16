FROM openjdk:17-jdk-slim

# 필요한 패키지 설치 및 로케일 설정
RUN apt-get update && \
    apt-get install -y locales wget unzip && \
    echo "ko_KR.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen && \
    apt-get clean

# Locale 환경변수 설정
ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

# 크롬 브라우저 설치
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb

# 크롬 버전 확인 및 해당 버전에 맞는 크롬 드라이버 다운로드 및 설치
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+') && \
    echo "Chrome version: $CHROME_VERSION" && \
    wget https://storage.googleapis.com/chrome-for-testing-public/$CHROME_VERSION/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip -d /usr/bin/ && \
    rm chromedriver-linux64.zip

# 환경변수 PATH에 크롬 드라이버 경로 추가
ENV PATH="/usr/bin/chromedriver-linux64:${PATH}"

RUN mkdir -p /app/images

# 애플리케이션 JAR 파일 복사
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]


