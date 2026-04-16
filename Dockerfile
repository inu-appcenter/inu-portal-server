FROM eclipse-temurin:17-jdk-jammy

# 패키지 저장소를 한국 미러(카카오)로 변경
RUN sed -i 's/archive.ubuntu.com/mirror.kakao.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirror.kakao.com/g' /etc/apt/sources.list

# 패키지 설치 및 로케일 설정
RUN apt-get update && \
    apt-get install -y --no-install-recommends locales wget unzip curl jq python3 python3-pip && \
    echo "ko_KR.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV LANG=ko_KR.UTF-8 \
    LANGUAGE=ko_KR:ko \
    LC_ALL=ko_KR.UTF-8

# 크롬 및 드라이버 설치
RUN wget -q -O /tmp/google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get update && \
    apt-get install -y --no-install-recommends /tmp/google-chrome.deb && \
    CHROME_VERSION=$(google-chrome --version | grep -oP '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+') && \
    CHROME_BUILD=$(echo "$CHROME_VERSION" | cut -d. -f1-3) && \
    DRIVER_VERSION=$(curl -s https://googlechromelabs.github.io/chrome-for-testing/latest-patch-versions-per-build.json | jq -r --arg b "$CHROME_BUILD" '.builds[$b].version') && \
    wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/$DRIVER_VERSION/linux64/chromedriver-linux64.zip && \
    unzip -q /tmp/chromedriver.zip -d /tmp && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/google-chrome.deb /tmp/chromedriver.zip /tmp/chromedriver-linux64 /var/lib/apt/lists/*

ENV PATH="/usr/local/bin:${PATH}"

# 파이썬 의존성 설치
COPY ./instagram-2gisik-scrapping/requirements.txt /tmp/requirements.txt
RUN python3 -m pip install --no-cache-dir --upgrade pip && \
    python3 -m pip install --no-cache-dir -r /tmp/requirements.txt && \
    rm -f /tmp/requirements.txt

RUN mkdir -p /intip/logs /intip/images
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]