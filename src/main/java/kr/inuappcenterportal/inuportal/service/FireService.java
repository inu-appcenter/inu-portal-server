package kr.inuappcenterportal.inuportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.DatatypeConverter;
import kr.inuappcenterportal.inuportal.domain.Fire;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.FireResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.FireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class FireService {
    private final WebClient webClient;
    private final FireRepository fireRepository;

    @Value("${aiUrl}")
    private String url;

    @Transactional
    public FireResponseDto drawImage(Member member, String prompt) throws JsonProcessingException {
        HashMap<String,Object > body = new HashMap<>();
        body.put("u_id",member.getId());
        body.put("prompt",prompt);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(body);
        log.info("횃불이 ai 이미지 생성 요청 파라미터 :{}",prompt);
        FireResponseDto fireResponseDto =webClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.set("Content-Type","application/json"))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.just(new MyException(MyErrorCode.BAD_REQUEST_FIRE_AI)))
                .bodyToMono(FireResponseDto.class)
                .block();
        Fire fire = Fire.builder().requestId(fireResponseDto.getRequest_id()).prompt(prompt).member(member).build();
        fireRepository.save(fire);
        return fireResponseDto;
    }



    @Transactional(readOnly = true)
    public Page<Fire> getFireImageList(Member member, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,4);
        return fireRepository.findByMemberOrderByIdDesc(member,pageable);
    }



}
