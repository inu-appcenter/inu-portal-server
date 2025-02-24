package kr.inuappcenterportal.inuportal.domain.weather.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.weather.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {
    private final WebClient webClient;
    private final RedisService redisService;
    @Value("${weatherKey}")
    private String weatherKey;

    private final String x = "54";
    private final String y = "123";

    @Scheduled(cron = "0 35 * * * *")
    public void getWeatherAPI(){
        getWeatherSky();
        getTemperature();
        getDust();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void getDay() {
        getDayData();
    }


    @PostConstruct
    public void initWeather(){
        WeatherResponseDto weatherResponseDto = getWeather();
        if(checkingNullData(weatherResponseDto)) {
            getDayData();
            getDust();
            getWeatherSky();
            getTemperature();
        }
    }
    private boolean checkingNullData(WeatherResponseDto weatherResponseDto){
        return weatherResponseDto.getTemperature()==null|| weatherResponseDto.getDay()==null|| weatherResponseDto.getSky()==null|| weatherResponseDto.getPm25Grade()==null|| weatherResponseDto.getPm10Value()==null|| weatherResponseDto.getPm10Grade()==null|| weatherResponseDto.getPm25Value()==null;
    }
    @Transactional
    public void getWeatherSky(){
        LocalDateTime t = LocalDateTime.now().minusMinutes(30);


        String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst"+
                "?ServiceKey="+weatherKey+
                "&pageNo=1"+
                "&numOfRows=20"+
                "&dataType=JSON"+
                "&base_date=" + t.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "&base_time=" + t.format(DateTimeFormatter.ofPattern("HHmm"))
                + "&nx=" + x
                + "&ny=" + y;

        JsonArray itemList = getJsonData(url);
        String sky = "";
        String pty = "";
        int index = 0;
        for (int i = 0; i < itemList.size(); i++) {
            JsonObject item = itemList.get(i).getAsJsonObject();
            String category = item.get("category").getAsString();
            if (category.equals("PTY")) {
                pty = item.get("fcstValue").getAsString();
                index = i;
                break;
            }
        }
        for (int i = index; i < itemList.size(); i++) {
            JsonObject item = itemList.get(i).getAsJsonObject();
            String category = item.get("category").getAsString();
            if (category.equals("SKY")) {
                sky = item.get("fcstValue").getAsString();
                break;
            }
        }
        String weather= redisService.getSky()==null?"맑음":redisService.getSky();
        if(pty.equals("0")){
            if(sky.equals("1")){
                weather="맑음";
            }
            else if(sky.equals("3")||sky.equals("4")){
                weather="구름";
            }
        }
        else{
            if(pty.equals("1")||pty.equals("5")){
                weather="비";
            }
            else if(pty.equals("2")||pty.equals("6")){
                weather="진눈깨비";
            }
            else if(pty.equals("3")||pty.equals("7")){
                weather="눈";
            }
        }
        redisService.storeSky(weather);
    }


    @Transactional
    public void getTemperature(){
        LocalDateTime t = LocalDateTime.now();
        String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst"+
                "?ServiceKey="+weatherKey+
                "&pageNo=1"+
                "&numOfRows=10"+
                "&dataType=JSON"+
                "&base_date=" + t.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "&base_time=" + t.format(DateTimeFormatter.ofPattern("HHmm"))
                + "&nx=" + x
                + "&ny=" + y;

        JsonArray itemList = getJsonData(url);

        String temperature = "";

        String pty = "";
        int index = 0;
        for (int i = 0; i < itemList.size(); i++) {
            JsonObject item = itemList.get(i).getAsJsonObject();
            String category = item.get("category").getAsString();
            if (category.equals("PTY")) {
                pty = item.get("obsrValue").getAsString();
                index = i;
                break;
            }
        }
        for (int i = index; i < itemList.size(); i++) {
            JsonObject item = itemList.get(i).getAsJsonObject();
            String category = item.get("category").getAsString();
            if (category.equals("T1H")) {
                temperature = item.get("obsrValue").getAsString();
                break;
            }
        }
        String weather="";
        redisService.storeTemperature(temperature);
        if(!pty.equals("0")){
            if(pty.equals("1")||pty.equals("5")){
                weather="비";
            }
            else if(pty.equals("2")||pty.equals("6")){
                weather="진눈깨비";
            }
            else if(pty.equals("3")||pty.equals("7")){
                weather="눈";
            }
            redisService.storeSky(weather);
        }
    }

    @Transactional
    public void getDust(){
        String url = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty"+
                "?serviceKey="+weatherKey+
                "&returnType=json"+
                "&numOfRows=100"+
                "&pageNo=1"+
                "&sidoName=%EC%9D%B8%EC%B2%9C"+
                "&ver=1.0";

        JsonObject item = getSongdoInformation(url);
        String pm10Value = "-1";
        String pm10Grade = "보통";
        String pm25Value = "-1";
        String pm25Grade = "보통";

        JsonElement pm10ValueElement = item.get("pm10Value");
        if (pm10ValueElement != null && !pm10ValueElement.isJsonNull()) {
            pm10Value = pm10ValueElement.getAsString();
        }

        JsonElement pm10GradeElement = item.get("pm10Grade");
        if (pm10GradeElement != null && !pm10GradeElement.isJsonNull()) {
            pm10Grade = getGrade(pm10GradeElement.getAsString());
        }

        JsonElement pm25ValueElement = item.get("pm25Value");
        if (pm25ValueElement != null && !pm25ValueElement.isJsonNull()) {
            pm25Value = pm25ValueElement.getAsString();
        }

        JsonElement pm25GradeElement = item.get("pm25Grade");
        if (pm25GradeElement != null && !pm25GradeElement.isJsonNull()) {
            pm25Grade = getGrade(pm25GradeElement.getAsString());
        }
        redisService.storeDust(pm10Value,pm10Grade,pm25Value,pm25Grade);
    }

    public String getGrade(String level){
        String grade="";
        if(level.equals("1")){
            grade="좋음";
        }
        else if(level.equals("2")){
            grade="보통";
        }
        else if(level.equals("3")){
            grade="나쁨";
        }
        else if(level.equals("4")){
            grade="매우나쁨";
        }
        return grade;
    }


    public JsonObject getSongdoInformation(String url) {
        String result = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.WEATHER_REQUEST_ERROR)))
                .bodyToMono(String.class)
                .block();
        JsonArray itemList;
        try {
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            JsonObject body = jsonObject.getAsJsonObject("response").getAsJsonObject("body");
            itemList = body.getAsJsonArray("items");
        }
        catch (Exception e){
            itemList = new JsonArray();
        }
        for (int i = 0; i < itemList.size(); i++) {
            JsonObject item = itemList.get(i).getAsJsonObject();
            if ("송도".equals(item.get("stationName").getAsString())) {
                return item;
            }
        }
        return null;
    }

    public JsonArray getJsonData(String url){
        String result = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.WEATHER_REQUEST_ERROR)))
                .bodyToMono(String.class)
                .block();
        try {
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            JsonObject body = jsonObject.getAsJsonObject("response").getAsJsonObject("body");
            JsonArray itemList = body.getAsJsonObject("items").getAsJsonArray("item");
            return itemList;
        }
        catch (Exception e){
            throw new RuntimeException("날씨 관련 api호출 중 문제 발생");
        }
    }

    @Transactional
    public void getDayData(){
        LocalDateTime t = LocalDateTime.now();
        String url = "http://apis.data.go.kr/B090041/openapi/service/RiseSetInfoService/getAreaRiseSetInfo" +
                "?location=%EC%9D%B8%EC%B2%9C"+
                "&locdate="+t.format(DateTimeFormatter.ofPattern("yyyyMMdd"))+
                "&serviceKey="+weatherKey;
        String result = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.WEATHER_REQUEST_ERROR)))
                .bodyToMono(String.class)
                .block();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(result)));
            document.getDocumentElement().normalize();
            NodeList itemList = document.getElementsByTagName("item");
            Element item = (Element) itemList.item(0);

            NodeList sunriseList = item.getElementsByTagName("sunrise");
            Node sunriseNode = sunriseList.item(0);
            String sunrise = sunriseNode.getTextContent();

            NodeList sunsetList = item.getElementsByTagName("sunset");
            Node sunsetNode = sunsetList.item(0);
            String sunset = sunsetNode.getTextContent();

            redisService.storeSun(sunrise,sunset);
        } catch (Exception e) {
            throw new RuntimeException("일출일몰 api호출 중 문제 발생");
        }
    }




    public WeatherResponseDto getWeather(){
        String sky = redisService.getSky();
        String temperature = redisService.getTemperature();
        Map<String,String> dusts = redisService.getDust();
        Map<String,String > sun = redisService.getSun();
        String day = isDaytime(sun.get("sunrise"),sun.get("sunset"))?"day":"night";
        return WeatherResponseDto.of(sky,temperature,dusts.get("pm10Value"),dusts.get("pm10Grade"),dusts.get("pm25Value"),dusts.get("pm25Grade"),day);
    }

    public boolean isDaytime(String sunrise, String sunset) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");
        LocalTime sunriseTime = LocalTime.parse(sunrise, timeFormatter);
        LocalTime sunsetTime = LocalTime.parse(sunset, timeFormatter);
        LocalTime now = LocalTime.now();
        return (now.isAfter(sunriseTime) && now.isBefore(sunsetTime));
    }


}
