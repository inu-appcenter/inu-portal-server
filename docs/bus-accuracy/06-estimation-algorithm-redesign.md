# [Phase 6 세부 분석] 과거 운행 이력 기반 연속 예측 알고리즘 재설계 및 고도화 명세

- **관련 이슈**: https://github.com/inu-appcenter/inu-portal-server/issues/516
- **대상 파일**: `inu-portal-server/src/main/java/.../bus/service/BusService.java`
- **작성일자**: 2026-10-12

---

## 1. 현행 알고리즘의 수학적 결함 분석

### 결함 1: LIMIT 1 조회로 인한 시간 불연속성 (Discontinuity / 널뛰기 현상)
```java
// 현행 방식
Optional<BusArrivalHistory> pastLogOpt = busArrivalHistoryRepository
        .findFirstByBstopIdAndRouteIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, routeId, startWindow, endWindow);
```
- 현행 코드는 현재 시각 $T$ 이후의 과거 도착 기록 중 최초 1건과의 차이를 계산합니다.
- **사례**:
  - 1주 전 실측 통과 시각이 14:02, 14:20인 경우:
  - 오늘 14:01에 조회 시: $14:02 - 14:01 = 1분$
  - 오늘 14:03에 조회 시: 14:02 기록이 지나갔으므로 다음 버스인 14:20이 매칭됨 -> $14:20 - 14:03 = 17분$!
  - **불과 2분 지났을 뿐인데 도착 예상 시간이 1분에서 17분으로 16분이나 점프(널뛰기)하는 치명적 불연속 발생**.

### 결함 2: 3분 고정 캐시로 인한 시간 정지 현상
- `medianSeconds`를 그대로 Redis에 캐싱하여, 2분이 지난 후 재조회해도 동일한 초가 반환되어 시간이 줄어들지 않음.

---

## 2. 카카오맵 / 네이버 지도 방식의 연속 예측 모델 설계

카카오맵과 네이버 지도는 버스가 고정 시간표가 아닌 배차 간격(Headway)으로 운행된다는 특성을 고려하여, **최근 통과 시점($T_{last}$) + 평균 배차 간격($H$) 슬라이딩 모델**을 적용합니다.

### 2.1 알고리즘 수학적 모델
1. **통계적 배차 간격 ($H_{stat}$)**:
   - 해당 정류소/노선의 과거 4주 동일 요일·시간대(현재 기준 $\pm 1$시간)의 평균 통과 간격 계산.
2. **오늘 마지막 통과 시각 ($T_{last}$)**:
   - 오늘(`LocalDate.now()`) 해당 정류소에 해당 노선이 마지막으로 도착/통과한 시각 조회.
3. **다음 도착 예상 시각 ($T_{target}$)**:
   - $T_{target} = T_{last} + H_{stat}$
   - 만약 현재 시각 $Now$가 이미 $T_{target}$을 초과했거나 오늘 첫차 대기인 경우:
     - 과거 4주 동일 시간대 통과 시각의 연속 분포 중앙값으로 보정.
4. **동적 슬라이딩 잔여 시간**:
   - $\text{RemainingSeconds} = \max(60, \text{Duration}(Now, T_{target}))$
   - 타임스탬프 기반 캐싱을 적용하여 캐시가 유지되는 3분 동안에도 재조회 시 흐른 시간만큼 정확히 차감되어 반환.

---

## 3. 정석적 구현 방안

- `BusService.calculateEstimatedArrivalForRoute`를 리팩토링하여:
  - 타임스탬프 슬라이딩 캐시(`targetArrivalEpochMs`) 적용
  - 최근 실측 통과 시점과 배차 간격을 융합한 부드러운 감속 모델 구현
  - `estimationNotice`: "과거 운행 패턴 통계 기반 예상"으로 안내
