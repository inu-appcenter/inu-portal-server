package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.course.dto.syllabus.SyllabusResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
            summary = "강의계획서 조회",
            description = """
                    개설 강의(CourseOffering) ID로 해당 강의의 강의계획서를 조회합니다.

                    - `content`는 강의계획서 본문 전체이며, 키는 원본 양식 그대로 한글입니다.
                    - `content` 내부 필드는 값이 없을 수 있습니다(null 허용).
                    - 값이 배열인 필드: `유의사항`, `주별수업계획`, `과제`, `전공능력가중치`, `_sections`, `_pages`
                    - 값이 객체(키-정수 맵)인 필드: `수업방식비율`, `기자재활용비율`, `성적평가비율`, `핵심역량가중치`
                    - `교재.주교재`, `교재.참고서적`는 서적 객체 배열, `교재.기타서적`는 자유 서술 문자열입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "강의계획서 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "강의계획서 조회 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "courseOfferingId": 1024,
                                                "content": {
                                                  "년도": 2026,
                                                  "학기": "2학기",
                                                  "출력일시": "2026-08-26 20:44",
                                                  "과목명": "자료구조",
                                                  "과목코드": "12345",
                                                  "이수구분": "전공필수",
                                                  "성적평가방법": "상대평가",
                                                  "부복수전공절대평가여부": "N",
                                                  "집중이수제구분": "일반",
                                                  "전화번호": "032-835-0000",
                                                  "요일교시강의실": "월5,6 7호관 101",
                                                  "면담가능시간": "수 14:00~16:00",
                                                  "원어강의구분": "국어",
                                                  "학과": "컴퓨터공학부",
                                                  "학년": "2",
                                                  "소속": "정보기술대학",
                                                  "교수": "홍길동",
                                                  "학점": "3",
                                                  "강의": "3",
                                                  "실습": "0",
                                                  "교과목개요및목적": "선형 및 비선형 자료구조와 알고리즘 복잡도를 학습한다.",
                                                  "수업목표": "주요 자료구조를 구현하고 응용할 수 있다.",
                                                  "수업진행방법": "이론 강의와 실습 병행",
                                                  "수업방식비율": { "강의": 60, "토론": 0, "세미나": 0, "실습": 40, "시청각": 0, "유인물": 0, "견학": 0, "기타": 0 },
                                                  "기자재활용비율": { "판서": 40, "OHP": 0, "슬라이드": 40, "차트": 0, "비디오": 0, "오디오": 0, "컴퓨터": 20, "기타": 0 },
                                                  "학습평가방법": "중간·기말 시험과 과제로 평가한다.",
                                                  "성적평가비율": { "시험": 60, "출석": 20, "과제": 20 },
                                                  "유의사항": [
                                                    "출석 성적: 20점 만점(학칙시행세칙 제56조 제2항)",
                                                    "실제 수업시간수의 1/3 이상 결석 시 학점 인정 불가"
                                                  ],
                                                  "교재": {
                                                    "주교재": [
                                                      { "교재명": "Data Structures and Algorithm Analysis", "저자": "Mark A. Weiss", "출판사": "Pearson", "발행년도": "2014" }
                                                    ],
                                                    "참고서적": [
                                                      { "교재명": "Introduction to Algorithms", "저자": "CLRS", "출판사": "MIT Press", "발행년도": "2009" }
                                                    ],
                                                    "기타서적": "강의자료 제공"
                                                  },
                                                  "주별수업계획": [
                                                    { "주차": 1, "내용": "강의 오리엔테이션, 복잡도 분석" },
                                                    { "주차": 2, "내용": "배열과 연결 리스트" }
                                                  ],
                                                  "과제": [
                                                    { "번호": 1, "과제명": "연결 리스트 구현", "제출일": "2026-09-21 월", "목표": "포인터 조작 숙달", "진행방법및유의사항": "표준 라이브러리 사용 금지", "참고자료": null }
                                                  ],
                                                  "장애학생학습지원": "요청 시 강의자료 사전 제공 및 시험시간 연장",
                                                  "핵심역량가중치": { "지식탐구": 3, "의사소통": null, "문제해결": 5, "창의융합": null, "협업인성": null, "도전창조": null },
                                                  "전공능력가중치": [
                                                    { "전공능력": "프로그래밍능력", "가중치": 5 },
                                                    { "전공능력": "문제해결능력", "가중치": 4 }
                                                  ],
                                                  "_sections": [1, 2, 3, 4, 5, 6, 7, 8, 9],
                                                  "_pages": [1, 3]
                                                }
                                              },
                                              "msg": "강의계획서 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 개설 강의의 강의계획서가 존재하지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<SyllabusResponseDto>> getSyllabus(
            @Parameter(
                    description = "조회할 개설 강의(CourseOffering)의 ID입니다.",
                    example = "1024",
                    required = true
            )
            @RequestParam Long courseOfferingId
    );
}
