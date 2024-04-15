FROM eclipse-temurin:17-jdk

# 필요한 의존성 설치
RUN apt-get update && \
    apt-get install -y wget unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 크롬 브라우저 설치
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 크롬 드라이버 설치
RUN wget https://storage.googleapis.com/chrome-for-testing-public/123.0.6312.122/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip -d /usr/local/bin/ && \
    rm chromedriver-linux64.zip

# 환경변수 PATH에 크롬 드라이버 경로 추가
ENV PATH="/usr/local/bin:${PATH}"

VOLUME /tmp
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]