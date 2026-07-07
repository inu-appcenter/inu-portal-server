package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseResponseDto;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Course", description = "Courses 관련 API")
public interface CourseApiSpecification {
    @Operation(
            summary = "Course 조회",
            description = "Course 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Course 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Course 조회 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "title": "운영체제",
                                                  "departmentCode": "COMPUTER_ENGINEERING",
                                                  "departmentName": "컴퓨터공학부",
                                                  "collegeCode": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                                  "collegeName": "정보기술대학",
                                                  "targetGradeCode": "THIRD",
                                                  "targetGradeName": "3학년",
                                                  "targetTermCode": "FIRST",
                                                  "targetTermName": "1학기"
                                                  "completionDivisionCode": "CORE_MAJOR",
                                                  "completionDivisionName": "전공핵심",
                                                  "credit": "3",
                                                  "content": "운영체제의 Process 구현, 동기화, 기억장치 운영, 자원분배, 시스템 보안 등에 대하여 연구하며, 대형컴퓨터의 사례연구와 실제 설계의 구성 능력을 배양한다.",
                                                  "active": true
                                                },
                                                {
                                                  "id": 2,
                                                  "title": "자료구조",
                                                  "departmentCode": "COMPUTER_ENGINEERING",
                                                  "departmentName": "컴퓨터공학부",
                                                  "collegeCode": "COLLEGE_OF_INFORMATION_TECHNOLOGY"
                                                  "collegeName": "정보기술대학",
                                                  "targetGradeCode": "SECOND",
                                                  "targetGradeName": "2학년",
                                                  "targetTermCode": "SECOND"
                                                  "targetTermName": "1학기"
                                                  "completionDivisionCode": "CORE_MAJOR",
                                                  "completionDivisionName": "전공핵심",
                                                  "credit": "3",
                                                  "content": "자료의 표현, 스택, 큐, 리스트, 트리, 그래프 등의 기본 자료구조와 알고리즘을 학습한다.",
                                                  "active": true
                                                }
                                              ],
                                              "msg": "강의 목록 조회 성공"
                                            }
                                            """
                            )
                    )
            )
    }
    )
    ResponseEntity<ResponseDto<List<CourseResponseDto>>> getCourses(@RequestParam(required = false) Department department);
}
