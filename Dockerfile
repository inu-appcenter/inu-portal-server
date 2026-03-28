FROM eclipse-temurin:17-jdk-jammy

# 필요한 패키지 설치 및 로케일 설정
RUN apt-get update && \
    apt-get install -y locales wget unzip curl jq && \
    echo "ko_KR.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Locale 환경변수 설정
ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

# 크롬 브라우저 설치
RUN wget -O /tmp/google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get update && \
    apt-get install -y /tmp/google-chrome.deb && \
    rm /tmp/google-chrome.deb && \
    rm -rf /var/lib/apt/lists/*

# 설치된 Chrome의 "빌드 버전(예: 146.0.7680)"에 맞는 실제 존재하는 chromedriver 버전 조회 후 설치
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+') && \
    CHROME_BUILD=$(echo "$CHROME_VERSION" | cut -d. -f1-3) && \
    DRIVER_VERSION=$(curl -s https://googlechromelabs.github.io/chrome-for-testing/latest-patch-versions-per-build.json | jq -r --arg b "$CHROME_BUILD" '.builds[$b].version') && \
    echo "Chrome version: $CHROME_VERSION" && \
    echo "Chrome build: $CHROME_BUILD" && \
    echo "Driver version: $DRIVER_VERSION" && \
    test "$DRIVER_VERSION" != "null" && \
    wget -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/$DRIVER_VERSION/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip -d /tmp && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /tmp/chromedriver-linux64

# 환경변수 PATH에 크롬 드라이버 경로 추가
ENV PATH="/usr/local/bin:${PATH}"

RUN mkdir -p /intip/logs /intip/images

# 애플리케이션 JAR 파일 복사
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]