package kr.inuappcenterportal.inuportal.course;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.ExcelParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CourseOverviewExcelRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExcelParserTest {

    @Test
    void 강의시간표_엑셀을_파싱한다() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("학수번호");
        header.createCell(1).setCellValue("담당교수");
        header.createCell(2).setCellValue("교과목명");
        header.createCell(3).setCellValue("성적평가");
        header.createCell(4).setCellValue("정원");

        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("0009062001");
        row.createCell(1).setCellValue("김정경");
        row.createCell(2).setCellValue("RISE");
        row.createCell(3).setCellValue("상대평가");
        row.createCell(4).setCellValue("2,500");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "timetable.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        ExcelParser parser = new ExcelParser();

        List<CourseOverviewExcelRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).subjectNumber()).isEqualTo("0009062001");
        assertThat(rows.get(0).professor()).isEqualTo("김정경");
        assertThat(rows.get(0).courseTitle()).isEqualTo("RISE");
        assertThat(rows.get(0).gradeEvaluation()).isEqualTo("상대평가");
        assertThat(rows.get(0).capacity()).isEqualTo(2500);
    }
}
