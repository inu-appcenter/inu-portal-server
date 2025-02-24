package kr.inuappcenterportal.inuportal.domain.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemDetail;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemPreview;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemRegister;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemUpdate;
import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import kr.inuappcenterportal.inuportal.domain.item.service.ItemService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "items", description = " 물품대여 API")
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "물품 저장", description = "헤더 Auth에 발급받은 토큰을, 바디에 {itemCategory,name,int 형태의 totalQuantity, deposit} 보내주세요. 이미지를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "물품 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto<Long>> register(@RequestPart @Valid ItemRegister itemRegister, @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        if(ObjectUtils.isEmpty(images)) images = new ArrayList<>();
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(itemService.register(itemRegister, images), "물품 등록 성공"));
    }

    @Operation(summary = "물품 상세 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물품 상세 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{itemId}")
    public ResponseEntity<ResponseDto<ItemDetail>> get(@PathVariable Long itemId) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(itemService.get(itemId), "물품 상세 조회 성공"));
    }

    @Operation(summary = "물품 수정", description = "헤더 Auth에 발급받은 토큰을 보내주세요. 바디에 {itemCategory,name,int 형태의 totalQuantity, deposit} , url 파라미터에 물품의 id를 보내주세요. 이미지를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "책 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @PutMapping("/{itemId}")
    public ResponseEntity<ResponseDto<Long>> update(@RequestPart @Valid ItemUpdate itemUpdate, @RequestPart(required = false) List<MultipartFile> images, @PathVariable Long itemId) throws IOException {
        if(ObjectUtils.isEmpty(images)) images = new ArrayList<>();
        itemService.update(itemUpdate, images,itemId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(itemId, "물품 수정 성공"));
    }

    @Operation(summary = "물품 삭제", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "책 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ResponseDto<Long>> delete(@PathVariable Long itemId) {
        itemService.delete(itemId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(itemId, "물품 삭제 성공"));
    }

    @Operation(summary = "물품 리스트 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요. 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "물품 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ResponseDto<List<ItemPreview>>> getList() {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(itemService.getList(), "물품 리스트 조회 성공"));
    }
}
