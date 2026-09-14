package kr.inuappcenterportal.inuportal.domain.image.controller;

import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController implements ImageApiSpecification {

    private final RedisService redisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> saveOnlyImage(@RequestPart List<MultipartFile> images) throws IOException {
        log.info("횃불이 이미지 저장 호출");
        redisService.saveFireImage(images);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(1L, "이미지 등록 성공"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFireImage(@PathVariable Long id) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf("image/webp"));
        return ResponseEntity.ok().headers(httpHeaders).body(redisService.getFireImage(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Long>> deleteFireImage(@PathVariable Long id) {
        log.info("횃불이 삭제 가져오기 호출 id:{}", id);
        redisService.deleteFireImage(id);
        return ResponseEntity.ok(ResponseDto.of(1L, "횃불이 이미지 삭제 성공"));
    }
}
