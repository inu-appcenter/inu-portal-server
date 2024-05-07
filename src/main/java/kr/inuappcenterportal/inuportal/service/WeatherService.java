package kr.inuappcenterportal.inuportal.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.dto.FireResponseDto;
import kr.inuappcenterportal.inuportal.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.proxy.annotation.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {
    private final WebClient webClient;
    private final RedisService redisService;
    @Value("${weatherKey}")
    private String weatherKey;

    private String x = "54";
    private String y = "123";

    @Scheduled(cron = "0 35 * * * *")
    public void getSky(){
        getWeatherSky();
    }


    @Scheduled(cron = "0 10 * * * *")
    public void getTem(){
        getTemperature();
    }

    @PostConstruct
    public void initWeather(){
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
        String sky = null;
        String pty = null;
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
        log.info("PTY : {}, SKY :{} ",pty,sky);
        String weather= null;
        if(pty.equals("0")){
            if(sky.equals("1")){
                weather="맑음";
            }
            else if(sky.equals("3")||sky.equals("4")){
                weather="구름";
            }
        }
        else {
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

        String temperature = null;

        String pty = null;
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
        String weather=null;
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

    public JsonArray getJsonData(String url){
        String result = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.WEATHER_REQUEST_ERROR)))
                .bodyToMono(String.class)
                .block();

        JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
        JsonObject body = jsonObject.getAsJsonObject("response").getAsJsonObject("body");
        JsonArray itemList = body.getAsJsonObject("items").getAsJsonArray("item");
        return itemList;
    }

    public WeatherResponseDto getWeather(){
        String sky = redisService.getSky();
        String temperature = redisService.getTemperature();
        return WeatherResponseDto.of(sky,temperature);
    }


}
