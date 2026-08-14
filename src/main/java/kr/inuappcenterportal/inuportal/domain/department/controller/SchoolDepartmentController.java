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

    @GetMapping
    public ResponseEntity<ResponseDto<List<SchoolDepartmentResponseDto>>> getDepartments() {
        return ResponseEntity.ok(ResponseDto.of(service.getActiveDepartments(), "학과 목록 조회 성공"));
    }
}
