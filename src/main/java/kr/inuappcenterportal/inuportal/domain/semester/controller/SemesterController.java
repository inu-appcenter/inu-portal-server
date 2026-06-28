package kr.inuappcenterportal.inuportal.domain.semester.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController implements SemesterApiSpecification {
}
