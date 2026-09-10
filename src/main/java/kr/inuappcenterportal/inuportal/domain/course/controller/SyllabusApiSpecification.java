package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Syllabus", description = "강의계획서 관련 API")
public interface SyllabusApiSpecification {

    @Operation(
            summary = "강의계획서 JSON 적재",
            description = """
                    [관리자 전용] 강의계획서 파싱본 JSON 파일을 업로드하여 Syllabus를 생성 또는 갱신합니다.

                    처리 방식:
                    - 업로드한 파일은 강의계획서 객체의 JSON 배열이어야 합니다.
                    - 각 항목의 `년도` + `학기` + `과목코드`로 해당 학기의 CourseOffering을 찾아 연결합니다.
                    - 같은 CourseOffering에 이미 강의계획서가 있으면 본문(content)을 갱신하고, 없으면 새로 저장합니다.
                    - 항목 단위로 트랜잭션을 분리하여 일부 항목이 실패해도 나머지 항목은 계속 처리합니다.
                    - 매칭되는 학기 또는 CourseOffering이 없는 항목은 로그를 남기고 스킵합니다.

                    요청:
                    - multipart/form-data 형식으로 `file` 필드에 .json 파일 하나를 전달합니다.

                    JSON 항목 예시:
                    ```json
                    [
                      {
                        "년도": 2024,
                        "학기": "2학기",
                        "과목코드": "12345",
                        "과목명": "자료구조",
                        "교수": "홍길동",
                        "교과목개요및목적": "..."
                      }
                    ]
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "강의계획서 적재 성공 (개별 항목 스킵 여부와 무관하게 배치가 끝나면 200)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "강의계획서 적재 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "강의계획서 적재 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JSON 파싱에 실패했거나 형식이 잘못되었습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<Void>> importSyllabus(
            @Parameter(
                    description = "강의계획서 파싱본 JSON 파일입니다. multipart/form-data의 file 필드로 전달합니다.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file
    );
}
