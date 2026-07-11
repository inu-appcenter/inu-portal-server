package kr.inuappcenterportal.inuportal.domain.semester.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Semester", description = "Semester 관련 API")
public interface SemesterApiSpecification {

    @Operation(
            summary = "학기 조회",
            description = "유효한 학기 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유효한 학기 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SemesterResponseDto.class)),
                            examples = @ExampleObject(
                                    name = "유효한 Semester 조회 응답 예시",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "status": "OPEN",
                                                "startDate": "2026-03-02",
                                                "endDate": "2026-06-21"
                                              },
                                              {
                                                "id": 2,
                                                "year": 2026,
                                                "term": "SUMMER",
                                                "status": "UPCOMING",
                                                "startDate": "2026-06-22",
                                                "endDate": "2026-07-12"
                                              }
                                            ]
                                            """
                            )
                    )
            )}
    )
    ResponseEntity<List<SemesterResponseDto>> getValidSemesters();
}
