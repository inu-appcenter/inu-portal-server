package kr.inuappcenterportal.inuportal.domain.department.controller;

import kr.inuappcenterportal.inuportal.domain.department.dto.SchoolDepartmentResponseDto;
import kr.inuappcenterportal.inuportal.domain.department.service.SchoolDepartmentService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/departments")
public class SchoolDepartmentController {
    private final SchoolDepartmentService service;

    @kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation.AgentExposed(
            name = "get_departments",
            description = "인천대학교 전체 공식 학과/학부 목록 및 학과 코드(88개 학과)를 조회합니다. 사용자가 비표준 줄임말(예: 컴공, 데사 등)을 사용하거나 정확한 학과명을 확인할 때 먼저 참조합니다.",
            capabilities = {"공식 학과 목록 확인", "학과 코드 조회"},
            triggerExamples = {"인천대 학과 목록 보여줘", "어떤 학과들이 개설되어 있어?"},
            cardTitle = "공식 학과 목록",
            requiresLogin = false
    )
    @GetMapping
    public ResponseEntity<ResponseDto<List<SchoolDepartmentResponseDto>>> getDepartments() {
        return ResponseEntity.ok(ResponseDto.of(service.getActiveDepartments(), "학과 목록 조회 성공"));
    }
}
