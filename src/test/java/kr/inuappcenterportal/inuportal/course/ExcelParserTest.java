package kr.inuappcenterportal.inuportal.course;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.ExcelParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CourseExcelRow;
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

        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("0009062001");
        row.createCell(1).setCellValue("김정경");
        row.createCell(2).setCellValue("RISE");

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

        List<CourseExcelRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).subjectNumber()).isEqualTo("0009062001");
        assertThat(rows.get(0).professor()).isEqualTo("김정경");
        assertThat(rows.get(0).courseTitle()).isEqualTo("RISE");
    }
}
