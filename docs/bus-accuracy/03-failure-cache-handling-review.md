# [Phase 3 세부 분석] 공공데이터 장애 시 캐시 오염 및 전파 메커니즘 차단 대책

- **관련 이슈**: https://github.com/inu-appcenter/inu-portal-server/issues/516
- **분석 대상 파일**: `inu-portal-server/src/main/java/.../bus/service/BusScheduler.java`, `BusApiService.java`, `BusService.java`
- **작성일자**: 2026-10-12

---

## 1. 현재의 치명적 결함: 장애 상황이 정상 무도착으로 둔갑하는 버그

`BusScheduler.java`의 수집 루프:

```java
// BusScheduler.java (현행)
List<BusArrivalItemDto> rawArrivals = busApiService.fetchBusArrivals(stop.getBstopId());

// ⚠️ 버그 지점: fetchBusArrivals가 429나 타임아웃 등으로 빈 리스트(List.of())를 반환했을 때!
List<BusArrivalItemDto> finalArrivals = busService.augmentWithEstimatedArrivals(stop.getBstopId(), rawArrivals);

// Redis에 40초 동안 저장
redisService.storeValueWithExpire("bus_realtime:" + stop.getBstopId(), json, 40, TimeUnit.SECONDS);
```

### 💥 문제의 연쇄 반응 (Cascading Failure):
1. 공공데이터포털이 429 Too Many Requests를 반환하거나 타임아웃이 발생하면 `rawArrivals`는 `[]`가 됩니다.
2. `augmentWithEstimatedArrivals`는 "현재 실시간 정보가 잡힌 노선이 하나도 없네?"라고 판단하여, **등록된 모든 노선에 대해 4주 통계 추정치를 계산**합니다.
3. 그 결과, 공공데이터가 마비된 상황에서 **실제로는 도로에 버스가 정상 운행 중임에도 불구하고, 모든 버스가 과거 4주 중앙값 시간으로 둔갑하여 사용자 화면에 노출**됩니다.
4. 사용자는 "공공데이터가 장애 상태"임을 알지 못하고, "앱이 엉뚱한 가짜 시간을 보여준다"며 심각한 불만을 품게 됩니다.

---

## 2. 정석적 개선 대책 (Action Items)

1. **`fetchBusArrivals`의 반환 상태 명확화**:
   - 단순 `List<BusArrivalItemDto>` 대신 결과 래퍼(`ArrivalFetchResult`)를 도입하거나, `null`(호출 실패/장애)과 `emptyList`(성공했으나 버스 없음)를 명확히 구분.
2. **장애 발생 시 통계 추정 강제 실행 차단**:
   - API 호출 실패 시에는 통계 추정치를 덮어씌우지 않고, 기존 캐시의 Stale 데이터를 허용 시간(최대 60초) 내에서만 안전하게 유지하거나 장애 상태를 기록.
3. **Stale 데이터 표기 지원**:
   - 캐시가 유지되는 동안은 `isStale=true` 플래그를 두어 프론트엔드가 신선하지 않은 데이터임을 인지할 수 있도록 보강.
