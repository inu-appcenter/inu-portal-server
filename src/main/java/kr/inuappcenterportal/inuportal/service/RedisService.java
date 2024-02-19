package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {

    private final RedisTemplate<String,byte[]> redisTemplateForImage;
    private final RedisTemplate<String,Boolean> redisTemplate;


    public boolean isFirstConnect(String address, Long postId){
        String key = address + "&"+ postId;
        log.info("check isFirstConnect key:{}",key);
        if(redisTemplate.hasKey(key)){
            return false;
        }
        else
            return true;
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
        redisTemplate.opsForValue().set(key,true);
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

    public byte[] findImages(Long postId, Integer imageId){
        String key = postId + "-" + imageId;
        log.info("이미지가져오기 key:{}",key);
        byte[] image = redisTemplateForImage.opsForValue().get(key);
        if(image==null){
            throw new MyNotFoundException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        return image;
    }

    public void updateImage(Long postId, List<MultipartFile> images, Integer imageCount) throws IOException {
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

    public void deleteImage(Long postId, Integer imageCount){
        for(int i = 0 ; i<imageCount;i++){
            String key = postId + "-" + (i+1);
            log.info("이미지 삭제 key:{}",key);
            redisTemplateForImage.delete(key);
        }
    }

}
