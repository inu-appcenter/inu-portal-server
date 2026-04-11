package kr.inuappcenterportal.inuportal.domain.academic.service;

import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AcademicSsvParserTest {

    private final AcademicSsvParser parser = new AcademicSsvParser();
    private static final String RS = String.valueOf((char) 30);
    private static final String US = String.valueOf((char) 31);
    private static final String NULL_MARKER = String.valueOf((char) 3);

    @Test
    void parseAcademicBasicInfo_parsesFirstDatasetRow() {
        String response = "SSV:UTF-8" + RS
                + "ErrorCode:int=0" + RS
                + "Dataset:DS_SREG101" + RS
                + "_RowType_" + US + "schregStGbn:string(32)" + US + "stuno:string(32)" + US + "korNm:string(32)"
                + US + "engNm:string(32)" + US + "entrDt:datetime(17)" + US + "flSchregModDt:datetime(17)"
                + US + "hgCd:string(32)" + US + "hgNm:string(64)" + US + "hgMjCd:string(32)" + US + "mjNm:string(64)"
                + US + "colgCd:string(32)" + US + "colgNm:string(64)" + US + "handpNo:string(32)"
                + US + "rrn:string(32)" + US + "profNm:string(32)" + US + "mrksAvg:string(32)"
                + US + "readmiYn:string(32)" + RS
                + "N" + US + "70" + US + "202001518" + US + "Hong Gil Dong" + US + "HONG GILDONG" + US + "20200302"
                + US + "20240229" + US + "0000077" + US + "Department of CSE" + US + "0000077" + US + "Computer Science"
                + US + "0000588" + US + "College of Engineering" + US + "01012345678" + US + "010101-3******"
                + US + "Advisor Kim" + US + "4.25" + US + NULL_MARKER + RS;

        AcademicBasicInfoResponseDto result = parser.parseAcademicBasicInfo(response);

        assertEquals("70", result.getEnrollmentStatusCode());
        assertEquals("202001518", result.getStudentId());
        assertEquals("Hong Gil Dong", result.getKoreanName());
        assertEquals("HONG GILDONG", result.getEnglishName());
        assertEquals("2020-03-02", result.getEntranceDate());
        assertEquals("2024-02-29", result.getLatestEnrollmentChangeDate());
        assertEquals("0000077", result.getDepartmentCode());
        assertEquals("Department of CSE", result.getDepartmentName());
        assertEquals("0000077", result.getMajorCode());
        assertEquals("Computer Science", result.getMajorName());
        assertEquals("0000588", result.getCollegeCode());
        assertEquals("College of Engineering", result.getCollegeName());
        assertEquals("01012345678", result.getMobilePhone());
        assertEquals("010101-3******", result.getResidentRegistrationNumberMasked());
        assertEquals("Advisor Kim", result.getAdvisorProfessorName());
        assertEquals("4.25", result.getGradeAverage());
        assertNull(result.getReadmissionYn());
    }

    @Test
    void parseCodeNameMap_parsesCodeLookupDataset() {
        String response = "SSV:UTF-8" + RS
                + "ErrorCode:int=0" + RS
                + "Dataset:DS_GEN_GBN" + RS
                + "_RowType_" + US + "code:string(32)" + US + "korCdNm:string(32)" + US + "fullNm:string(32)" + RS
                + "N" + US + "1" + US + "Male" + US + "Male" + RS
                + "N" + US + "2" + US + "Female" + US + "Female" + RS;

        Map<String, String> result = parser.parseCodeNameMap(response, "DS_GEN_GBN");

        assertEquals("Male", result.get("1"));
        assertEquals("Female", result.get("2"));
    }

    @Test
    void parseDepartmentNameMap_parsesDepartmentXmlDatasets() {
        String response = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                    <Parameters>
                        <Parameter id="ErrorCode" type="int">0</Parameter>
                    </Parameters>
                    <Dataset id="DS_BAIM001_01">
                        <ColumnInfo>
                            <Column id="deptCd" type="string" size="32"/>
                            <Column id="deptNm" type="string" size="32"/>
                        </ColumnInfo>
                        <Rows>
                            <Row>
                                <Col id="deptCd">0000587</Col>
                                <Col id="deptNm">University</Col>
                            </Row>
                            <Row>
                                <Col id="deptCd">0000077</Col>
                                <Col id="deptNm">Department of CSE</Col>
                            </Row>
                        </Rows>
                    </Dataset>
                    <Dataset id="DS_BAIM001_02">
                        <ColumnInfo>
                            <Column id="deptCd" type="string" size="32"/>
                            <Column id="deptNm" type="string" size="32"/>
                        </ColumnInfo>
                        <Rows>
                            <Row>
                                <Col id="deptCd">I000</Col>
                                <Col id="deptNm">College of Information Technology</Col>
                            </Row>
                        </Rows>
                    </Dataset>
                </Root>
                """;

        Map<String, String> result = parser.parseDepartmentNameMap(response);

        assertEquals("University", result.get("0000587"));
        assertEquals("Department of CSE", result.get("0000077"));
        assertEquals("College of Information Technology", result.get("I000"));
    }
}
