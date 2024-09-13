package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {

    private final RedisTemplate<String,byte[]> redisTemplateForImage;
    private final RedisTemplate<String,String> redisTemplate;


    public boolean isFirstConnect(String address, Long postId){
        String key = address + "&"+ postId;
        log.info("check isFirstConnect key:{}",key);
        return !redisTemplate.hasKey(key);
    }

    public void insertAddress(String address, Long postId){
        String key = address + "&" + postId;
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime endTime = currentTime
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        Duration subTime = Duration.between(currentTime, endTime);
        long expireTime = subTime.getSeconds();
        log.info("만료시간: {}",expireTime);
        redisTemplate.opsForValue().set(key,"1");
        redisTemplate.expire(key,expireTime, TimeUnit.SECONDS);
    }

    public void saveImage(Long postId, List<MultipartFile> images) throws IOException {
        for(int i = 0 ; i<images.size();i++){
            byte[] bytes = images.get(i).getBytes();
            String key = postId + "-" + (i+1);
            log.info("이미지 저장 key:{}",key);
            redisTemplateForImage.opsForValue().set(key,bytes);
        }
    }

    public void saveFireImage(List<MultipartFile> images) throws IOException {
        for(int i = 0 ; i<images.size();i++){
            byte[] bytes = images.get(i).getBytes();
            String key = "fire" + "-" + (i+1);
            log.info("이미지 저장 key:{}",key);
            redisTemplateForImage.opsForValue().set(key,bytes);
        }
    }

    public byte[] getFireImage(Long id){
        String key = "fire" + "-" + id;
        log.info("이미지가져오기 key:{}",key);
        byte[] image = redisTemplateForImage.opsForValue().get(key);
        if(image==null){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        return image;
    }

    public void deleteFireImage(Long id){
            String key = "fire" + "-" + id;
            log.info("이미지 삭제 key:{}",key);
            if(!redisTemplateForImage.hasKey(key)){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
            redisTemplateForImage.delete(key);
    }


    public byte[] findImages(Long postId, Long imageId){
        String key = postId + "-" + imageId;
        log.info("이미지가져오기 key:{}",key);
        byte[] image = redisTemplateForImage.opsForValue().get(key);
        if(image==null){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        return image;
    }

    public void updateImage(Long postId, List<MultipartFile> images, long imageCount) throws IOException {
        for(int i = 0 ; i<imageCount;i++){
            String key = postId + "-" + (i+1);
            redisTemplateForImage.delete(key);
        }
        for(int i = 0 ; i<images.size();i++){
            String key = postId + "-" + (i+1);
            byte[] bytes = images.get(i).getBytes();
            redisTemplateForImage.opsForValue().set(key,bytes);
        }
    }

    public void deleteImage(Long postId, long imageCount){
        for(int i = 0 ; i<imageCount;i++){
            String key = postId + "-" + (i+1);
            log.info("이미지 삭제 key:{}",key);
            redisTemplateForImage.delete(key);
        }
    }


    public void storeFireAiImage(String image,Long id){
        image = image.substring(2,image.length()-1);
        String key = "FireAi-" + id;
        log.info("이미지 저장 key:{}",key);
        redisTemplate.opsForValue().set(key,image);
        redisTemplate.expire(key,7, TimeUnit.DAYS);
    }

    public String getFireAiImage(Long id){
        String key = "FireAi-" + id;
        String image = redisTemplate.opsForValue().get(key);
        if(image==null){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        return image;
    }

    public void storeMeal(String cafeteria,int day, int num,String menu){
        String key = cafeteria+"-"+day+"-"+num;
        redisTemplate.opsForValue().set(key,menu);
    }

    public String getMeal(String cafeteria,int day, int num){
        String key = cafeteria+"-"+day+"-"+num;
        return redisTemplate.opsForValue().get(key);
    }

    public void storeSky(String sky){
        log.info("날씨저장 하늘 : {}",sky);
        redisTemplate.opsForValue().set("sky",sky);
    }

    public void storeTemperature(String temperature){
        log.info("기온 저장 온도 : {}",temperature);
        redisTemplate.opsForValue().set("temperature",temperature);
    }

    public String getSky(){
        return redisTemplate.opsForValue().get("sky");
    }

    public String getTemperature(){
        return redisTemplate.opsForValue().get("temperature");
    }

    public void storeDust(String pm10Value, String pm10Grade,String pm25Value,String pm25Grade){
        redisTemplate.opsForValue().set("pm10Value",pm10Value);
        redisTemplate.opsForValue().set("pm10Grade",pm10Grade);
        redisTemplate.opsForValue().set("pm25Value",pm25Value);
        redisTemplate.opsForValue().set("pm25Grade",pm25Grade);
    }

    public Map<String,String> getDust(){
        Map<String,String> dusts = new HashMap<>();
        dusts.put("pm10Value",redisTemplate.opsForValue().get("pm10Value"));
        dusts.put("pm10Grade",redisTemplate.opsForValue().get("pm10Grade"));
        dusts.put("pm25Value",redisTemplate.opsForValue().get("pm25Value"));
        dusts.put("pm25Grade",redisTemplate.opsForValue().get("pm25Grade"));
        return dusts;
    }

    public void storeSun(String sunrise, String sunset){
        redisTemplate.opsForValue().set("sunrise",sunrise);
        redisTemplate.opsForValue().set("sunset",sunset);
    }




}
