FROM docker.elastic.co/elasticsearch/elasticsearch:8.12.2

# Nori 형태소 분석기 플러그인 설치
RUN bin/elasticsearch-plugin install --batch analysis-nori

# 교내 전용 사용자 사전 및 동의어 사전 파일 복사
COPY config/userdict_ko.txt /usr/share/elasticsearch/config/
COPY config/synonyms_ko.txt /usr/share/elasticsearch/config/
