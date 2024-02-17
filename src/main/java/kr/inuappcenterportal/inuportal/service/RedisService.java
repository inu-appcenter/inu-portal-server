package kr.inuappcenterportal.inuportal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {

    private final RedisTemplate<String,String> redisTemplate;

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
        redisTemplate.expire(key,expireTime, TimeUnit.SECONDS);
    }

}
