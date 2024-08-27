package kr.inuappcenterportal.inuportal.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
    public void getSky(){
        getWeatherSky();
    }


    @Scheduled(cron = "0 35 * * * *")
    public void getTem(){
        getTemperature();
    }
    @Scheduled(cron = "0 30 * * * *")
    public void getCrawlDust(){
        getDust();
    }


    @PostConstruct
    public void initWeather(){
        getDust();
        getWeatherSky();
        getTemperature();
    }
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
        String weather= "";
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

        String temperature = "20";

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
        log.info(result.toString());
        try {
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            JsonObject body = jsonObject.getAsJsonObject("response").getAsJsonObject("body");
            JsonArray itemList = body.getAsJsonObject("items").getAsJsonArray("item");
            return itemList;
        }
        catch (Exception e){
            log.info("날씨 관련 api호출 중 문제 발생 ");
            return new JsonArray();
        }
    }



    public WeatherResponseDto getWeather(){
        String sky = redisService.getSky();
        String temperature = redisService.getTemperature();
        Map<String,String> dusts = redisService.getDust();
        return WeatherResponseDto.of(sky,temperature,dusts.get("pm10Value"),dusts.get("pm10Grade"),dusts.get("pm25Value"),dusts.get("pm25Grade"));
    }


}
