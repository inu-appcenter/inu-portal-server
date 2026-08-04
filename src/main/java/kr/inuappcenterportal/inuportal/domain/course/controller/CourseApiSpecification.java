package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.response.CourseResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Course", description = "Courses 관련 API")
public interface CourseApiSpecification {
    @Operation(
            summary = "강의 조회",
            description = "강의 조회합니다."
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
                                                  "targetTermName": "1학기",
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
                                                  "collegeCode": "COLLEGE_OF_INFORMATION_TECHNOLOGY",
                                                  "collegeName": "정보기술대학",
                                                  "targetGradeCode": "SECOND",
                                                  "targetGradeName": "2학년",
                                                  "targetTermCode": "SECOND",
                                                  "targetTermName": "1학기",
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
    ResponseEntity<ResponseDto<List<CourseResponseDto>>> getCourses(
            @Parameter(
                    description = "학과명. 영문 enum명 또는 한글 학과명 모두 가능(편의상 스웨거에서는 한국어로 통일)",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "국어국문학과",
                                    "영어영문학과",
                                    "독어독문학과",
                                    "불어불문학과",
                                    "일본지역문화학과",
                                    "중어중국학과",
                                    "수학과",
                                    "물리학과",
                                    "화학과",
                                    "패션산업학과",
                                    "해양학과",
                                    "사회복지학과",
                                    "미디어커뮤니케이션학과",
                                    "문헌정보학과",
                                    "창의인재개발학과",
                                    "행정학과",
                                    "정치외교학과",
                                    "경제학과",
                                    "Global Trade & Service 학부",
                                    "소비자학과",
                                    "에너지화학공학과",
                                    "전기공학과",
                                    "전자공학부",
                                    "산업경영공학과",
                                    "신소재공학과",
                                    "기계공학과",
                                    "바이오-로봇시스템공학과",
                                    "안전공학과",
                                    "컴퓨터공학부",
                                    "정보통신공학과",
                                    "임베디드시스템공학과",
                                    "경영학부",
                                    "데이터과학과",
                                    "세무회계학과",
                                    "조형예술학부",
                                    "한국화전공",
                                    "서양화전공",
                                    "디자인학부",
                                    "공연예술학과",
                                    "스포츠과학부",
                                    "운동건강학부",
                                    "국어교육과",
                                    "영어교육과",
                                    "일어교육과",
                                    "수학교육과",
                                    "체육교육과",
                                    "유아교육과",
                                    "역사교육과",
                                    "윤리교육과",
                                    "도시행정학과",
                                    "도시환경공학부(건설환경공학전공)",
                                    "도시환경공학부(환경공학전공)",
                                    "도시공학과",
                                    "도시건축학부(건축공학전공)",
                                    "도시건축학부(도시건축학전공)",
                                    "생명과학부(생명과학전공)",
                                    "생명과학부(분자의생명전공)",
                                    "생명공학부(생명공학전공)",
                                    "생명공학부(나노바이오공학전공)",
                                    "자유전공학부",
                                    "동북아국제통상전공",
                                    "스마트물류공학전공",
                                    "IBE전공",
                                    "법학부"
                            },
                            example = "컴퓨터공학부"
                    )
            )
            @RequestParam(required = false) String department);
}
