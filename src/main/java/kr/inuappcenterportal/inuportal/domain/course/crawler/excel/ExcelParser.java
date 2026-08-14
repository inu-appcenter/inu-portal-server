package kr.inuappcenterportal.inuportal.domain.course.crawler.excel;

import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CourseOverviewExcelRow;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ExcelParser {
    public List<CourseOverviewExcelRow> parse(MultipartFile file) {
        try {
            return parse(file.getInputStream());
        } catch (IOException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
    }

    public List<CourseOverviewExcelRow> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseWorkbook(workbook);
        } catch (IOException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
    }

    private List<CourseOverviewExcelRow> parseWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(1);
        if (headerRow == null) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        Map<String, Integer> headerIndex = readHeaderIndex(headerRow);

        List<CourseOverviewExcelRow> rows = new ArrayList<>();

        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            String subjectNumber = getString(row, headerIndex, "학수번호");
            if (subjectNumber == null || subjectNumber.isBlank()) {
                continue;
            }

            rows.add(new CourseOverviewExcelRow(
                    getString(row, headerIndex, "대학(원)"),
                    getString(row, headerIndex, "학과(부)"),
                    getString(row, headerIndex, "학년"),
                    getString(row, headerIndex, "이수구분"),
                    getString(row, headerIndex, "이수영역"),
                    subjectNumber,
                    getString(row, headerIndex, "교과목명"),
                    getString(row, headerIndex, "교과목명(영문)"),
                    getString(row, headerIndex, "담당교수"),
                    getString(row, headerIndex, "강의실"),
                    getString(row, headerIndex, "시간표(교시)"),
                    getString(row, headerIndex, "시간표(시간)"),
                    getString(row, headerIndex, "교시유형"),
                    getInteger(row, headerIndex, "학점"),
                    getString(row, headerIndex, "수업구분"),
                    getString(row, headerIndex, "수업유형"),
                    getString(row, headerIndex, "집중이수제"),
                    getString(row, headerIndex, "성적평가"),
                    getInteger(row, headerIndex, "정원"),
                    getString(row, headerIndex, "원어강의구분")
            ));
        }
        return rows;
    }

    private Map<String, Integer> readHeaderIndex(Row headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();

        for (Cell cell : headerRow) {
            String headerName = readCellAsString(cell);
            if (headerName != null && !headerName.isBlank()) {
                headerIndex.put(headerName.trim(), cell.getColumnIndex());
            }
        }

        validateRequiredHeaders(headerIndex);
        return headerIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of("학수번호", "담당교수");

        for (String requiredHeader : requiredHeaders) {
            if (!headerIndex.containsKey(requiredHeader)) {
                throw new MyException(MyErrorCode.INVALID_INPUT);
            }
        }
    }

    private String getString(Row row, Map<String, Integer> headerIndex, String headerName) {
        Integer columnIndex = headerIndex.get(headerName);
        if (columnIndex == null) {
            return null;
        }

        return readCellAsString(row.getCell(columnIndex));
    }

    private Integer getInteger(Row row, Map<String, Integer> headerIndex, String headerName) {
        String value = getString(row, headerIndex, headerName);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.replace(".0", "").trim());
        } catch (NumberFormatException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter(Locale.KOREA);
        String value = formatter.formatCellValue(cell);

        return value == null ? null : value.trim();
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            String value = readCellAsString(cell);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }

        return true;
    }
}
