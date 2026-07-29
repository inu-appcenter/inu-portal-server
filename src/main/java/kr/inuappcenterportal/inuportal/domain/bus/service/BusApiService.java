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

    private static final String ARRIVAL_API_URL = "https://apis.data.go.kr/6280000/busArrivalService/getAllRouteBusArrivalList";
    private static final String ROUTE_SECTION_API_URL = "https://apis.data.go.kr/6280000/busRouteService/getBusRouteSectionList";

    public List<BusArrivalItemDto> fetchBusArrivals(String bstopId) {
        if (busApiKey == null || busApiKey.isBlank()) {
            log.warn("인천 버스 API Key가 설정되지 않았습니다.");
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

    public List<BusRouteStopDto> fetchRouteStops(String routeId) {
        if (busApiKey == null || busApiKey.isBlank()) {
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

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0) != null) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
