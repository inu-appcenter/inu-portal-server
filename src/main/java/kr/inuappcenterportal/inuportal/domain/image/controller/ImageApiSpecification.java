package kr.inuappcenterportal.inuportal.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Images", description = "횃불이 이미지 API")
public interface ImageApiSpecification {

    @Operation(
            summary = "횃불이 이미지 등록",
            description = "multipart/form-data의 images 필드로 이미지 파일들을 전달하면 저장 후 번호를 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이미지 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> saveOnlyImage(
            @Parameter(
                    description = "등록할 횃불이 이미지 파일들입니다.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    )
            )
            @RequestPart List<MultipartFile> images
    ) throws IOException;

    @Operation(
            summary = "횃불이 이미지 가져오기",
            description = "url 변수에 가져올 횃불이의 번호를 보내주세요"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "횃불이 이미지 가져오기 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 이미지 번호입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<byte[]> getFireImage(
            @Parameter(
                    name = "id",
                    description = "가져올 횃불이 이미지의 번호",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long id
    );

    @Operation(
            summary = "횃불이 이미지 삭제",
            description = "url 변수에 삭제할 횃불이의 번호를 보내주세요"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "횃불이 삭제 가져오기 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 이미지 번호입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> deleteFireImage(
            @Parameter(
                    name = "id",
                    description = "삭제할 횃불이 이미지의 번호",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long id
    );
}
