# [Phase 2 세부 분석] 공공데이터 API 응답과 서버 DTO 전달 정합성 전수 조사

- **관련 이슈**: https://github.com/inu-appcenter/inu-portal-server/issues/516
- **분석 대상 파일**: `inu-portal-server/src/main/java/.../bus/service/BusApiService.java`, `BusService.java`
- **작성일자**: 2026-10-12

---

## 1. 공공데이터포털 인천 버스 도착정보 API 스키마 검증

- **API 엔드포인트**: `https://apis.data.go.kr/6280000/busArrivalService/getAllRouteBusArrivalList`
- **핵심 필드 명세**:
  - `ARRIVALESTIMATETIME`: 도착예정시간 (단위: **초**)
  - `REST_STOP_COUNT`: 남은 정류소 수
  - `DIRCD`: 운행방향코드 (0: 상행, 1: 하행, ...)
  - `LATEST_STOP_NAME`: 버스 현재 위치 정류소명
  - `BUS_NUM_PLATE`: 차량 번호판 (예: 인천70바1234)
  - `ROUTENO`: 노선 번호 (문자열, 예: "8", "6-1", "순환41")

---

## 2. 식별된 데이터 정합성 결함 및 이상치

1. **도착 예정 시간 이상치**:
   - 기점에서 버스가 배차 대기 중이거나 GPS 신호가 불량할 때 `ARRIVALESTIMATETIME`이 빈 문자열, `0`, 또는 `9999` 같은 특이값이 유입되는 현상.
   - 현행 코드는 단순 문자열 파싱만 수행하여 비정상치에 대한 검증 필터링이 취약함.
2. **`ROUTENO` 누락 및 동적 조회 지연**:
   - 공공데이터포털 응답 중 간혹 `ROUTENO`가 빈 문자열이거나 내부 시스템 ID(예: `165000012`) 형태로 내려오는 케이스가 존재함.
   - `fetchRouteNoDynamically`를 단건 동기 호출(`block()`)하면서 지연이 누적되고 실패 시 빈 문자열로 남아 프론트엔드 매핑이 실패함.
3. **복수 정류소 병합 시 중복 제거 키 불안정**:
   - `bstopId`가 콤마로 여러 개 들어올 때, 동일 차량/노선이 정류소 간 경계에서 중복 집계되는 문제.

---

## 3. 정석적 개선 대책 (Action Items)

1. **입력 데이터 유효성 검증(Validation Layer)** 강화:
   - `ARRIVALESTIMATETIME`이 정상적인 양수(1초 이상 7200초 이하)이고, `REST_STOP_COUNT`가 존재하는 정상 데이터만 실시간 도착 목록으로 승인.
2. **노선 마스터 인메모리 캐시 사전 탑재**:
   - 등록된 모든 `BusRouteSection`의 `routeId -> routeNo` 매핑을 영구 인메모리 맵으로 유지하여 동적 호출 실패를 원천 방지.
