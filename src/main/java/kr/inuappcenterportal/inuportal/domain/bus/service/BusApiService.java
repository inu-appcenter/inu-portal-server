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

    @Value("${busApiKey}")
    private String busApiKey;

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
                    String xmlResp = webClient.get().uri(URI.create(idUrl)).retrieve().bodyToMono(String.class).block();
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

            String xmlResponse = webClient.get()
                    .uri(URI.create(nameUrl))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xmlResponse != null && !xmlResponse.isBlank()) {
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



        try {
            String encodedKey = URLEncoder.encode(busApiKey, StandardCharsets.UTF_8);
            String url = String.format("%s?serviceKey=%s&bstopId=%s&pageNo=1&numOfRows=30",
                    ARRIVAL_API_URL, busApiKey, bstopId);

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xmlResponse == null || xmlResponse.isBlank()) {
                return List.of();
            }

            return parseArrivalXml(xmlResponse);
        } catch (Exception e) {
            log.error("버스 도착 정보 API 호출 실패 (bstopId: {})", bstopId, e);
            return List.of();
        }
    }

    public List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto> fetchRoutesViaStation(String bstopId) {
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || bstopId == null || bstopId.isBlank()) {
            return List.of();
        }

        try {
            String url = String.format("%s?serviceKey=%s&bstopId=%s&pageNo=1&numOfRows=100",
                    STATION_VIA_ROUTE_API_URL, busApiKey, bstopId.trim());

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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
        if (!IS_API_ENABLED || busApiKey == null || busApiKey.isBlank() || routeId == null) {
            return null;
        }

        try {
            String url = String.format("%s?serviceKey=%s&routeId=%s&pageNo=1&numOfRows=10",
                    ROUTE_INFO_API_URL, busApiKey, routeId.trim());

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xmlResponse == null || xmlResponse.isBlank()) {
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

                String gapNotice = null;
                if (!minGap.isBlank() && !maxGap.isBlank()) {
                    gapNotice = String.format("배차간격 | %s ~ %s분", minGap, maxGap);
                } else if (!minGap.isBlank()) {
                    gapNotice = String.format("배차간격 | %s분", minGap);
                }

                if (timeNotice != null && gapNotice != null) {
                    return timeNotice + "\n" + gapNotice;
                } else if (timeNotice != null) {
                    return timeNotice;
                } else if (gapNotice != null) {
                    return gapNotice;
                }
            }
        } catch (Exception e) {
            log.error("버스 노선 배차간격/운행시간 API 호출 실패 (routeId: {})", routeId, e);
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

            String xmlResponse = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xmlResponse == null || xmlResponse.isBlank()) {
                return List.of();
            }

            return parseRouteStopXml(xmlResponse);
        } catch (Exception e) {
            log.error("버스 노선 정류장 목록 API 호출 실패 (routeId: {})", routeId, e);
            return List.of();
        }
    }

    private List<BusArrivalItemDto> parseArrivalXml(String xmlData) throws Exception {
        List<BusArrivalItemDto> result = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

        NodeList items = doc.getElementsByTagName("itemList");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

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
                    .routeId(getTagValue("ROUTEID", item))
                    .routeNo(getTagValue("ROUTENO", item))
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
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String seqStr = getTagValue("PATHSEQ", item);
            if (seqStr.isBlank()) {
                seqStr = getTagValue("SEQ", item);
            }

            String latStr = getTagValue("LAT", item);
            if (latStr.isBlank()) {
                latStr = getTagValue("Y", item);
            }

            String lngStr = getTagValue("LNG", item);
            if (lngStr.isBlank()) {
                lngStr = getTagValue("X", item);
            }

            Integer seq = seqStr.isBlank() ? i + 1 : Integer.parseInt(seqStr);
            Double lat = latStr.isBlank() ? null : Double.parseDouble(latStr);
            Double lng = lngStr.isBlank() ? null : Double.parseDouble(lngStr);

            result.add(BusRouteStopDto.builder()
                    .seq(seq)
                    .bstopId(getTagValue("BSTOPID", item))
                    .bstopName(getTagValue("BSTOPNM", item))
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

            Double lat = latStr.isBlank() ? null : Double.parseDouble(latStr);
            Double lng = lngStr.isBlank() ? null : Double.parseDouble(lngStr);

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

