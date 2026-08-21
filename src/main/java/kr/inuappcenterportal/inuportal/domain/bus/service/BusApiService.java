package kr.inuappcenterportal.inuportal.domain.bus.service;

import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteStopDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusApiService {

    private final WebClient webClient;

    @Value("${weatherKey:${busApiKey:}}")
    private String busApiKey;




    private static final java.util.Map<String, String> ALLOC_GAP_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final com.github.benmanes.caffeine.cache.Cache<String, List<BusArrivalItemDto>> ARRIVAL_CACHE = 
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(20, java.util.concurrent.TimeUnit.SECONDS)
                    .maximumSize(1000)
                    .build();

    private static final java.util.Map<String, List<BusArrivalItemDto>> ARRIVAL_FALLBACK_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    // API 키 만료 시 false로 변경하여 공공데이터포털 연동 일시 중단 가능
    private static final boolean IS_API_ENABLED = true;

    private static final String ARRIVAL_API_URL = "https://apis.data.go.kr/6280000/busArrivalService/getAllRouteBusArrivalList";
    private static final String ROUTE_SECTION_API_URL = "https://apis.data.go.kr/6280000/busRouteService/getBusRouteSectionList";
    private static final String ROUTE_INFO_API_URL = "https://apis.data.go.kr/6280000/busRouteService/getBusRouteId";
    private static final String ROUTE_NO_SEARCH_API_URL = "https://apis.data.go.kr/6280000/busRouteService/getBusRouteNo";
    private static final String STOP_SEARCH_API_URL = "https://apis.data.go.kr/6280000/busStationService/getBusStationNmList";

    private static final String STOP_ID_SEARCH_API_URL = "https://apis.data.go.kr/6280000/busStationService/getBusStationIdList";
    private static final String STATION_VIA_ROUTE_API_URL = "https://apis.data.go.kr/6280000/busStationService/getBusStationViaRouteList";


    public List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto> searchBusStops(String keyword) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String trimmed = keyword.trim();
        List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto> results = new ArrayList<>();
        boolean isNumeric = trimmed.matches("^[0-9]+$");

        try {
            // 1. 숫자인 경우 정류소 ID/번호 조회 (getBusStationIdList)
            if (isNumeric) {
                try {
                    String idUrl = String.format("%s?serviceKey=%s&bstopId=%s&pageNo=1&numOfRows=30",
                            STOP_ID_SEARCH_API_URL, busApiKey, trimmed);
                    String xmlResp = executeGetXml(idUrl);
                    if (xmlResp != null && !xmlResp.isBlank()) {
                        results.addAll(parseBusStopXml(xmlResp));
                    }
                } catch (Exception ex) {
                    log.debug("정류소 ID 검색 실패, 명칭 검색으로 진행: {}", ex.getMessage());
                }
            }

            // 2. 정류소명 검색 (getBusStationNmList)
            String encodedKeyword = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
            String nameUrl = String.format("%s?serviceKey=%s&bstopNm=%s&pageNo=1&numOfRows=50",
                    STOP_SEARCH_API_URL, busApiKey, encodedKeyword);

            String xmlResponse = executeGetXml(nameUrl);
            if (xmlResponse != null && !xmlResponse.isBlank()) {
                if (xmlResponse.contains("<cmmMsgHeader>") || xmlResponse.contains("<returnAuthMsg>")) {
                    log.warn("공공데이터포털 API 에러 응답 수신 (keyword: {}): {}", keyword, xmlResponse.replaceAll("\\s+", " "));
                }
                results.addAll(parseBusStopXml(xmlResponse));
            }


            // 중복 bstopId 제거
            return results.stream()
                    .filter(distinctByKey(kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto::getBstopId))
                    .toList();

        } catch (Exception e) {
            log.error("정류소 검색 API 호출 실패 (keyword: {})", keyword, e);
            return results.stream()
                    .filter(distinctByKey(kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto::getBstopId))
                    .toList();
        }
    }

    private static <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }



    public List<BusArrivalItemDto> fetchBusArrivals(String bstopId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank()) {
            return List.of();
        }

        List<BusArrivalItemDto> cached = ARRIVAL_CACHE.getIfPresent(bstopId);
        if (cached != null) {
            return cached;
        }

        try {
            String trimmedStopId = bstopId.trim();
            String encodedKey = URLEncoder.encode(busApiKey, StandardCharsets.UTF_8);
            String encodedBstopId = URLEncoder.encode(trimmedStopId, StandardCharsets.UTF_8);
            String url = String.format("%s?serviceKey=%s&bstopId=%s&pageNo=1&numOfRows=30",
                    ARRIVAL_API_URL, encodedKey, encodedBstopId);

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class, e -> {
                        if (e.getStatusCode().value() == 429) {
                            log.warn("공공데이터 API 호출 한도 초과 (429) - bstopId: {}", bstopId);
                            return reactor.core.publisher.Mono.empty(); // Fallback 처리를 위해 빈 Mono 반환
                        }
                        return reactor.core.publisher.Mono.error(e);
                    })
                    .block();

            if (xmlResponse == null || xmlResponse.isBlank()) {
                return ARRIVAL_FALLBACK_CACHE.getOrDefault(bstopId, List.of());
            }

            List<BusArrivalItemDto> result = parseArrivalXml(xmlResponse);
            if (!result.isEmpty()) {
                ARRIVAL_CACHE.put(bstopId, result);
                ARRIVAL_FALLBACK_CACHE.put(bstopId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("버스 도착 정보 API 호출 실패 (bstopId: {})", bstopId, e);
            return ARRIVAL_FALLBACK_CACHE.getOrDefault(bstopId, List.of());
        }
    }

    private String executeGetXml(String url) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = webClient.get()
                        .uri(URI.create(url))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (response != null && !response.isBlank()) {
                    return response;
                }
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                if (e.getStatusCode().value() == 429) {
                    log.warn("공공데이터 API 429 Rate Limit 감지. {}ms 대기 후 재시도합니다 (시도: {}/{})", attempt * 1000, attempt, maxRetries);
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(attempt * 1000L);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    return null;
                }
                log.warn("API 요청 HTTP 오류 (status: {}, url: {})", e.getStatusCode(), url);
                return null;
            } catch (Exception e) {
                log.warn("API 요청 중 오류 발생 (url: {}): {}", url, e.getMessage());
                return null;
            }
        }
        return null;
    }

    public List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto> fetchRoutesViaStation(String bstopId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || bstopId == null || bstopId.isBlank()) {
            return List.of();
        }

        try {
            String url = String.format("%s?serviceKey=%s&bstopId=%s&pageNo=1&numOfRows=100",
                    STATION_VIA_ROUTE_API_URL, busApiKey, bstopId.trim());

            String xmlResponse = executeGetXml(url);
            if (xmlResponse == null || xmlResponse.isBlank()) {
                return List.of();
            }

            List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto> routes = new ArrayList<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            NodeList items = doc.getElementsByTagName("itemList");
            if (items.getLength() == 0) items = doc.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String routeId = getTagValue("ROUTEID", item);
                if (routeId.isBlank()) routeId = getTagValue("routeId", item);

                String routeNo = getTagValue("ROUTENO", item);
                if (routeNo.isBlank()) routeNo = getTagValue("routeNo", item);

                if (!routeId.isBlank()) {
                    routes.add(kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto.builder()
                            .routeId(routeId)
                            .routeNo(routeNo)
                            .build());
                }
            }
            return routes;
        } catch (Exception e) {
            log.error("정류소 경유 노선 API 호출 실패 (bstopId: {})", bstopId, e);
            return List.of();
        }
    }

    public String fetchAllocGap(String routeId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || routeId == null || routeId.isBlank()) {
            return null;
        }

        String cached = ALLOC_GAP_CACHE.get(routeId);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        try {
            String url = String.format("%s?serviceKey=%s&routeId=%s&pageNo=1&numOfRows=10",
                    ROUTE_INFO_API_URL, busApiKey, routeId.trim());

            String xmlResponse = executeGetXml(url);
            if (xmlResponse == null || xmlResponse.isBlank()) {
                ALLOC_GAP_CACHE.put(routeId, "");
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            NodeList items = doc.getElementsByTagName("itemList");
            if (items.getLength() == 0) items = doc.getElementsByTagName("item");

            if (items.getLength() > 0) {
                Element item = (Element) items.item(0);
                String minGap = getTagValue("MIN_ALLOCGAP", item);
                if (minGap.isBlank()) minGap = getTagValue("MIN_ALLOC_GAP", item);

                String maxGap = getTagValue("MAX_ALLOCGAP", item);
                if (maxGap.isBlank()) maxGap = getTagValue("MAX_ALLOC_GAP", item);

                String fbus = getTagValue("FBUS_DEPHMS", item);
                String lbus = getTagValue("LBUS_DEPHMS", item);

                String timeNotice = null;
                if (!fbus.isBlank() && fbus.length() >= 4 && !lbus.isBlank() && lbus.length() >= 4) {
                    timeNotice = String.format("운행시간 | %s:%s ~ %s:%s",
                            fbus.substring(0, 2), fbus.substring(2, 4),
                            lbus.substring(0, 2), lbus.substring(2, 4));
                }

                String allocNotice = null;
                if (!minGap.isBlank() && !maxGap.isBlank()) {
                    if (minGap.equals(maxGap)) {
                        allocNotice = String.format("배차간격 | %s분", minGap);
                    } else {
                        allocNotice = String.format("배차간격 | %s ~ %s분", minGap, maxGap);
                    }
                }

                String result = null;
                if (timeNotice != null && allocNotice != null) {
                    result = timeNotice + "\n" + allocNotice;
                } else if (timeNotice != null) {
                    result = timeNotice;
                } else if (allocNotice != null) {
                    result = allocNotice;
                }

                ALLOC_GAP_CACHE.put(routeId, result != null ? result : "");
                return result;
            }
            ALLOC_GAP_CACHE.put(routeId, "");
        } catch (Exception e) {
            log.warn("버스 노선 배차간격/운행시간 API 호출 실패 (routeId: {}): {}", routeId, e.getMessage());
            ALLOC_GAP_CACHE.put(routeId, "");
        }
        return null;
    }


    public List<BusRouteStopDto> fetchRouteStops(String routeId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank()) {
            return List.of();
        }

        try {
            String url = String.format("%s?serviceKey=%s&routeId=%s&pageNo=1&numOfRows=200",
                    ROUTE_SECTION_API_URL, busApiKey, routeId);

            String xmlResponse = executeGetXml(url);
            if (xmlResponse == null || xmlResponse.isBlank()) {
                return List.of();
            }

            return parseRouteStopXml(xmlResponse);
        } catch (Exception e) {
            log.error("버스 노선 정류장 목록 API 호출 실패 (routeId: {})", routeId, e);
            return List.of();
        }
    }


    private static final java.util.Map<String, String> DYNAMIC_ROUTE_NO_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public String fetchRouteNoDynamically(String routeId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || routeId == null || routeId.isBlank()) {
            return "";
        }

        String cached = DYNAMIC_ROUTE_NO_CACHE.get(routeId);
        if (cached != null) {
            return cached;
        }

        try {
            String url = String.format("%s?serviceKey=%s&routeId=%s&pageNo=1&numOfRows=1",
                    ROUTE_INFO_API_URL, busApiKey, routeId.trim());
            String xml = executeGetXml(url);
            if (xml != null && !xml.isBlank()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
                NodeList items = doc.getElementsByTagName("itemList");
                if (items.getLength() == 0) items = doc.getElementsByTagName("item");
                if (items.getLength() > 0) {
                    Element item = (Element) items.item(0);
                    String routeNo = getTagValue("ROUTENO", item);
                    if (!routeNo.isBlank()) {
                        DYNAMIC_ROUTE_NO_CACHE.put(routeId, routeNo);
                        return routeNo;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("노선 정보 API로부터 routeNo 동적 조회 실패 (routeId: {})", routeId, e);
        }

        DYNAMIC_ROUTE_NO_CACHE.put(routeId, "");
        return "";
    }

    private List<BusArrivalItemDto> parseArrivalXml(String xmlData) throws Exception {
        List<BusArrivalItemDto> result = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

        NodeList items = doc.getElementsByTagName("itemList");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String routeId = getTagValue("ROUTEID", item);
            String routeNo = getTagValue("ROUTENO", item);
            if ((routeNo.isBlank() || routeNo.matches("^[0-9]+$")) && !routeId.isBlank()) {
                String dynamicNo = fetchRouteNoDynamically(routeId);
                if (!dynamicNo.isBlank()) {
                    routeNo = dynamicNo;
                }
            }

            result.add(BusArrivalItemDto.builder()
                    .arrivalEstimateTime(getTagValue("ARRIVALESTIMATETIME", item))
                    .bstopId(getTagValue("BSTOPID", item))
                    .busId(getTagValue("BUSID", item))
                    .busNumPlate(getTagValue("BUS_NUM_PLATE", item))
                    .congestion(getTagValue("CONGESTION", item))
                    .dircd(getTagValue("DIRCD", item))
                    .lastBusYn(getTagValue("LASTBUSYN", item))
                    .latestStopId(getTagValue("LATEST_STOP_ID", item))
                    .latestStopName(getTagValue("LATEST_STOP_NAME", item))
                    .lowTpCd(getTagValue("LOW_TP_CD", item))
                    .remaindSeat(getTagValue("REMAIND_SEAT", item))
                    .restStopCount(getTagValue("REST_STOP_COUNT", item))
                    .routeId(routeId)
                    .routeNo(routeNo)
                    .build());
        }
        return result;
    }

    private List<BusRouteStopDto> parseRouteStopXml(String xmlData) throws Exception {
        List<BusRouteStopDto> result = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

        NodeList items = doc.getElementsByTagName("itemList");
        if (items.getLength() == 0) items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String seqStr = getTagValue("BSTOPSEQ", item);
            if (seqStr.isBlank()) seqStr = getTagValue("PATHSEQ", item);
            if (seqStr.isBlank()) seqStr = getTagValue("SEQ", item);

            String latStr = getTagValue("LAT", item);
            if (latStr.isBlank()) latStr = getTagValue("POSY", item);
            if (latStr.isBlank()) latStr = getTagValue("Y", item);

            String lngStr = getTagValue("LNG", item);
            if (lngStr.isBlank()) lngStr = getTagValue("POSX", item);
            if (lngStr.isBlank()) lngStr = getTagValue("X", item);

            Integer seq = seqStr.isBlank() ? i + 1 : Integer.parseInt(seqStr);
            Double rawLat = latStr.isBlank() ? null : Double.parseDouble(latStr);
            Double rawLng = lngStr.isBlank() ? null : Double.parseDouble(lngStr);

            Double lat = rawLat;
            Double lng = rawLng;

            if (rawLat != null && rawLng != null) {
                // POSX(rawLng), POSY(rawLat) 형태의 TM 좌표를 WGS84 위경도로 변환
                double[] converted = kr.inuappcenterportal.inuportal.domain.bus.util.GeoCoordinateConverter.tmToWgs84(rawLng, rawLat);
                lat = converted[0];
                lng = converted[1];
            }

            String bstopId = getTagValue("BSTOPID", item);
            if (bstopId.isBlank()) bstopId = getTagValue("bstopId", item);

            String bstopNm = getTagValue("BSTOPNM", item);
            if (bstopNm.isBlank()) bstopNm = getTagValue("bstopNm", item);

            result.add(BusRouteStopDto.builder()
                    .seq(seq)
                    .bstopId(bstopId)
                    .bstopName(bstopNm)
                    .latitude(lat)
                    .longitude(lng)
                    .build());
        }
        return result;
    }


    private List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto> parseBusStopXml(String xmlData) throws Exception {
        List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto> result = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

        NodeList items = doc.getElementsByTagName("itemList");
        if (items.getLength() == 0) {
            items = doc.getElementsByTagName("item");
        }

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String bstopId = getTagValue("BSTOPID", item);
            if (bstopId.isBlank()) bstopId = getTagValue("bstopId", item);

            String bstopNm = getTagValue("BSTOPNM", item);
            if (bstopNm.isBlank()) bstopNm = getTagValue("bstopNm", item);

            String bstopNo = getTagValue("SHORT_BSTOPID", item);
            if (bstopNo.isBlank()) bstopNo = getTagValue("BSTOPNO", item);
            if (bstopNo.isBlank()) bstopNo = getTagValue("bstopNo", item);

            String adminNm = getTagValue("ADMINNM", item);
            if (adminNm.isBlank()) adminNm = getTagValue("adminNm", item);

            String latStr = getTagValue("LAT", item);
            if (latStr.isBlank()) latStr = getTagValue("POSY", item);
            if (latStr.isBlank()) latStr = getTagValue("GPS_LATI", item);
            if (latStr.isBlank()) latStr = getTagValue("lat", item);

            String lngStr = getTagValue("LNG", item);
            if (lngStr.isBlank()) lngStr = getTagValue("POSX", item);
            if (lngStr.isBlank()) lngStr = getTagValue("GPS_LONG", item);
            if (lngStr.isBlank()) lngStr = getTagValue("lng", item);

            Double rawLat = latStr.isBlank() ? null : Double.parseDouble(latStr);
            Double rawLng = lngStr.isBlank() ? null : Double.parseDouble(lngStr);

            Double lat = rawLat;
            Double lng = rawLng;

            if (rawLat != null && rawLng != null) {
                double[] converted = kr.inuappcenterportal.inuportal.domain.bus.util.GeoCoordinateConverter.tmToWgs84(rawLng, rawLat);
                lat = converted[0];
                lng = converted[1];
            }

            if (!bstopId.isBlank() && !bstopNm.isBlank()) {
                result.add(kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto.builder()
                        .bstopId(bstopId)
                        .bstopName(bstopNm)
                        .bstopNo(bstopNo)
                        .adminNm(adminNm)
                        .latitude(lat)
                        .longitude(lng)
                        .build());
            }
        }
        return result;
    }


    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0) != null) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}

