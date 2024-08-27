# Fedora를 기반 이미지로 시작
FROM fedora:latest
# 필요한 패키지 설치 및 로케일 설정
RUN dnf install -y glibc-langpack-ko && \
    dnf clean all

# Locale 환경변수 설정
ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8
# JDK 17 설치
RUN dnf install -y java-17-openjdk-devel && \
    dnf clean all

# JAVA_HOME 설정
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# JDK 버전 확인
RUN java -version
# 크롬 브라우저 설치
RUN yum install -y wget unzip && \
    wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm && \
    yum localinstall -y google-chrome-stable_current_x86_64.rpm && \
    rm google-chrome-stable_current_x86_64.rpm

# 크롬 버전 확인 및 해당 버전에 맞는 크롬 드라이버 다운로드 및 설치
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+') && \
    echo "Chrome version: $CHROME_VERSION" && \
    wget https://storage.googleapis.com/chrome-for-testing-public/$CHROME_VERSION/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip -d /usr/bin/ && \
    rm chromedriver-linux64.zip

# 환경변수 PATH에 크롬 드라이버 경로 추가
ENV PATH="/usr/bin/chromedriver-linux64:${PATH}"

VOLUME /tmp
COPY ./build/libs/inu-portal-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
