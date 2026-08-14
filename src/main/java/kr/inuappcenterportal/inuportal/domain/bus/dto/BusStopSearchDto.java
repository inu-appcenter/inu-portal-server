package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "정류소 검색 결과 DTO")
public class BusStopSearchDto {

    @Schema(description = "정류소 ID (공공데이터)", example = "164000395")
    private String bstopId;

    @Schema(description = "정류소 명칭", example = "인천대입구역 2번출구")
    private String bstopName;

    @Schema(description = "정류소 번호 (5자리 단축번호)", example = "38395")
    private String bstopNo;

    @Schema(description = "행정구역/위치 정보", example = "연수구")
    private String adminNm;

    @Schema(description = "위도 (Kakao 지도)", example = "37.385213")
    private Double latitude;

    @Schema(description = "경도 (Kakao 지도)", example = "126.643891")
    private Double longitude;

    @Schema(description = "등록된 별칭 (있는 경우)", example = "인입")
    private String stopAlias;
}
