package kr.inuappcenterportal.inuportal.service;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.DatatypeConverter;
import kr.inuappcenterportal.inuportal.domain.Fire;
import kr.inuappcenterportal.inuportal.dto.FireResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.FireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireService {
    private final WebClient webClient;
    private final RedisService redisService;
    private final FireRepository fireRepository;

    @Value("${aiUrl}")
    private String initAiUrl;

    private static String url;

    @PostConstruct
    public void initAiUrl(){
        url = initAiUrl;
        log.info("ai 이미지 요청 url init url : {}",url);
    }
    @Transactional
    public Long drawImage(String param){
        List<String> body = new ArrayList<>();
        body.add(param);
        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("data", body);
        log.info("횃불이 ai 이미지 생성 요청 파라미터 :{}",param);
        FireResponseDto fireResponseDto =webClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.set("Content-Type","application/json"))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.NOT_FOUND_AI_URI)))
                .bodyToMono(FireResponseDto.class)
                .block();
        log.info("횃불이 ai 이미지 생성완료 걸린시간 :{}", fireResponseDto.getDuration());
        Fire fire = Fire.builder().duration(fireResponseDto.getDuration()).averageDuration(fireResponseDto.getAverage_duration()).build();
        Long id = fireRepository.save(fire).getId();
        redisService.storeFireAiImage(fireResponseDto.getData().get(0),id);
        return id;
    }

    public byte[] getFireAiImage(Long id){
        return DatatypeConverter.parseBase64Binary(redisService.getFireAiImage(id));
    }

    public void changeUri(String uri){
        url = uri;
        log.info("uri 변경 완료 변경된 요청 uri: {}",url);
    }



}
