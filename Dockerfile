# ── 1단계: jar를 레이어별 폴더로 풀기 ──
# 라이브러리(거의 안 바뀜)와 우리 코드(매번 바뀜)를 나눠서, 코드만 바뀌면 작은 레이어만 새로 만들기 위함
# 결과: /extract/{dependencies, spring-boot-loader, snapshot-dependencies, application}
# Spring Boot 3.3 이상으로 올리면 layertools가 deprecated → `-Djarmode=tools extract --layers --launcher`로 변경 필요
FROM eclipse-temurin:17-jdk-jammy AS extractor
WORKDIR /extract
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── 2단계: 실제 실행 이미지 ──
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

# 1단계에서 풀어둔 jar를 잘 안 바뀌는 순서대로 복사
# 각 COPY가 레이어 하나가 되므로, 평소 배포에서는 마지막 application(약 7MB)만 새로 만들어지고
# 위의 dependencies(약 188MB)는 캐시·서버에 있는 것을 재사용함
COPY --from=extractor /extract/dependencies/ /app/
COPY --from=extractor /extract/spring-boot-loader/ /app/
COPY --from=extractor /extract/snapshot-dependencies/ /app/
COPY --from=extractor /extract/application/ /app/

# jar 대신 풀어둔 폴더(/app)에서 Spring Boot 런처로 실행
# compose의 working_dir(/intip) 영향을 받지 않도록 -cp는 절대경로로 지정
# 런처 클래스 경로는 Spring Boot 3.2부터 org.springframework.boot.loader.launch.JarLauncher
ENTRYPOINT ["java", "-cp", "/app", "org.springframework.boot.loader.launch.JarLauncher"]