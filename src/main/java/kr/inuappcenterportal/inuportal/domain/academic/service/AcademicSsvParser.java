package kr.inuappcenterportal.inuportal.domain.academic.service;

import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoResponseDto;
import kr.inuappcenterportal.inuportal.domain.academic.exception.AcademicException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AcademicSsvParser {

    static final String RECORD_SEPARATOR = String.valueOf((char) 30);
    static final String UNIT_SEPARATOR = String.valueOf((char) 31);
    static final String NULL_MARKER = String.valueOf((char) 3);
    private static final String ROW_TYPE = "_RowType_";

    public AcademicBasicInfoResponseDto parseAcademicBasicInfo(String responseBody) {
        if (responseBody == null || !responseBody.contains("ErrorCode:int=0")) {
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to fetch academic info.");
        }

        Map<String, String> row = parseFirstRow(responseBody, "DS_SREG101");

        return AcademicBasicInfoResponseDto.builder()
                .studentId(row.get("stuno"))
                .koreanName(row.get("korNm"))
                .englishName(row.get("engNm"))
                .enrollmentStatusCode(row.get("schregStGbn"))
                .entranceClassificationCode(row.get("entrClsfGbn"))
                .entranceTypeCode(row.get("entrGbn"))
                .entranceDate(formatNexacroDate(row.get("entrDt")))
                .latestEnrollmentChangeCode(row.get("flSchregModGbn"))
                .latestEnrollmentChangeDate(formatNexacroDate(row.get("flSchregModDt")))
                .genderCode(row.get("genGbn"))
                .birthDate(formatNexacroDate(row.get("birthDt")))
                .departmentCode(row.get("hgCd"))
                .departmentName(row.get("hgNm"))
                .majorCode(row.get("hgMjCd"))
                .majorName(row.get("mjNm"))
                .collegeGroupCode(row.get("colgGrscCd"))
                .collegeCode(row.get("colgCd"))
                .collegeName(row.get("colgNm"))
                .courseCode(row.get("corsGbn"))
                .semesterSequenceCode(row.get("hySeqGbn"))
                .completedSemesterCode(row.get("cnpassHySeqGbn"))
                .completedSemesterName(row.get("cptnTmNm"))
                .nationalityCode(row.get("natGbn"))
                .militaryStatusCode(row.get("milFinishGbn"))
                .readmissionYn(row.get("readmiYn"))
                .earlyGraduationYn(row.get("earlyGrdtYn"))
                .graduationExpectedYn(row.get("grdtExpcYn"))
                .bcrmstConnectionYn(row.get("bcrmstConnYn"))
                .capacityIoCode(row.get("capaIoGbn"))
                .skillStandardCode(row.get("skilStdGbn"))
                .advisorProfessorName(row.get("profNm"))
                .acquiredCredits(row.get("acqHp"))
                .gradeAverage(row.get("mrksAvg"))
                .completedSemesterCount(row.get("mrksCptnTmCnt"))
                .mobilePhone(row.get("handpNo"))
                .residentRegistrationNumberMasked(row.get("rrn"))
                .build();
    }

    public Map<String, String> parseCodeNameMap(String responseBody, String datasetName) {
        Map<String, String> codeNameMap = new LinkedHashMap<>();
        for (Map<String, String> row : parseRows(responseBody, datasetName)) {
            String code = row.get("code");
            String name = firstNonNull(row.get("fullNm"), row.get("korCdNm"), row.get("codeNm"));
            if (code != null && name != null) {
                codeNameMap.put(code, name);
            }
        }
        return codeNameMap;
    }

    public Map<String, String> parseDepartmentNameMap(String xmlResponse, String... datasetNames) {
        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to fetch academic code info.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlResponse)));
            NodeList datasets = document.getElementsByTagName("Dataset");
            Map<String, String> codeNameMap = new LinkedHashMap<>();
            for (int i = 0; i < datasets.getLength(); i++) {
                Element dataset = (Element) datasets.item(i);
                if (!shouldParseDataset(dataset.getAttribute("id"), datasetNames)) {
                    continue;
                }

                NodeList rows = dataset.getElementsByTagName("Row");
                for (int rowIndex = 0; rowIndex < rows.getLength(); rowIndex++) {
                    Element row = (Element) rows.item(rowIndex);
                    String code = findColumnValue(row, "deptCd");
                    String name = findColumnValue(row, "deptNm");
                    if (code != null && name != null) {
                        codeNameMap.put(code, name);
                    }
                }
            }

            if (!codeNameMap.isEmpty()) {
                return codeNameMap;
            }
        } catch (Exception exception) {
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to fetch academic code info.");
        }

        throw new AcademicException(HttpStatus.NOT_FOUND, "Academic code info not found.");
    }

    Map<String, String> parseFirstRow(String responseBody, String datasetName) {
        List<Map<String, String>> rows = parseRows(responseBody, datasetName);
        if (rows.isEmpty()) {
            throw new AcademicException(HttpStatus.NOT_FOUND, "Academic info not found.");
        }
        return rows.get(0);
    }

    List<Map<String, String>> parseRows(String responseBody, String datasetName) {
        String[] records = responseBody.split(RECORD_SEPARATOR, -1);
        int datasetIndex = -1;

        for (int i = 0; i < records.length; i++) {
            if (("Dataset:" + datasetName).equals(records[i])) {
                datasetIndex = i;
                break;
            }
        }

        if (datasetIndex < 0) {
            throw new AcademicException(HttpStatus.NOT_FOUND, "Academic info not found.");
        }

        String[] columns = null;
        boolean hasRowTypeColumn = false;
        List<Map<String, String>> rows = new ArrayList<>();

        for (int i = datasetIndex + 1; i < records.length; i++) {
            String record = records[i];

            if (record == null || record.isBlank()) {
                continue;
            }

            if (record.startsWith("Dataset:")) {
                break;
            }

            if (isMetadataRecord(record)) {
                continue;
            }

            if (columns == null) {
                String[] parsedColumns = parseColumns(record);
                if (parsedColumns.length == 0) {
                    continue;
                }

                hasRowTypeColumn = ROW_TYPE.equals(parsedColumns[0]);
                columns = hasRowTypeColumn
                        ? Arrays.copyOfRange(parsedColumns, 1, parsedColumns.length)
                        : parsedColumns;
                continue;
            }

            Map<String, String> row = parseRow(columns, record, hasRowTypeColumn);
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }

        return rows;
    }

    private String[] parseColumns(String record) {
        String[] rawColumns = record.split(UNIT_SEPARATOR, -1);
        String[] normalizedColumns = new String[rawColumns.length];

        for (int i = 0; i < rawColumns.length; i++) {
            normalizedColumns[i] = normalizeColumnName(rawColumns[i]);
        }

        return normalizedColumns;
    }

    private Map<String, String> parseRow(String[] columns, String record, boolean hasRowTypeColumn) {
        String[] tokens = record.split(UNIT_SEPARATOR, -1);
        int startIndex = hasRowTypeColumn || tokens.length == columns.length + 1 ? 1 : 0;

        if (tokens.length < columns.length + startIndex) {
            return Map.of();
        }

        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) {
            String columnName = columns[i];
            if ("phtFile1".equals(columnName) || "phtFile2".equals(columnName)) {
                continue;
            }
            row.put(columnName, normalize(tokens[i + startIndex]));
        }
        return row;
    }

    private boolean isMetadataRecord(String record) {
        return !record.contains(UNIT_SEPARATOR)
                || record.startsWith("ErrorCode")
                || record.startsWith("ErrorMsg")
                || record.startsWith("ConstColumnInfo")
                || record.startsWith("RowType")
                || record.startsWith("ColumnInfo");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank() || NULL_MARKER.equals(value)) {
            return null;
        }
        return value;
    }

    private String normalizeColumnName(String column) {
        if (column == null || ROW_TYPE.equals(column)) {
            return column;
        }

        int typeSeparatorIndex = column.indexOf(':');
        if (typeSeparatorIndex > -1) {
            return column.substring(0, typeSeparatorIndex);
        }

        return column;
    }

    private String formatNexacroDate(String value) {
        if (value == null) {
            return null;
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return value;
        }

        return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String findColumnValue(Element row, String columnId) {
        NodeList columns = row.getElementsByTagName("Col");
        for (int i = 0; i < columns.getLength(); i++) {
            Element column = (Element) columns.item(i);
            if (columnId.equals(column.getAttribute("id"))) {
                String text = column.getTextContent();
                return text == null || text.isBlank() ? null : text;
            }
        }
        return null;
    }

    private boolean shouldParseDataset(String datasetId, String... datasetNames) {
        if (datasetNames == null || datasetNames.length == 0) {
            return true;
        }

        for (String datasetName : datasetNames) {
            if (datasetName != null && datasetName.equals(datasetId)) {
                return true;
            }
        }

        return false;
    }
}
