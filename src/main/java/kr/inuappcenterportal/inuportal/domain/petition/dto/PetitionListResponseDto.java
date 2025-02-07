package kr.inuappcenterportal.inuportal.domain.petition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "총학생회 청원 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class PetitionListResponseDto {
    @Schema(description = "총학생회 청원 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "제목",example = "제목")
    private String title;
    @Schema(description = "좋아요")
    private Long like;
    @Schema(description = "조회수")
    private Long view;
    @Schema(description = "이미지수")
    private Long imageCount;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    @Builder
    private PetitionListResponseDto(Long id, String title, Long like, Long view, Long imageCount, LocalDate createDate, LocalDateTime modifiedDate){
        this.id = id;
        this.title = title;
        this.like = like;
        this.view = view;
        this.imageCount = imageCount;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
    }

    public static PetitionListResponseDto of(Petition petition){
        return PetitionListResponseDto.builder()
                .id(petition.getId())
                .title(petition.getTitle())
                .like(petition.getGood())
                .view(petition.getView())
                .imageCount(petition.getImageCount())
                .createDate(petition.getCreateDate())
                .modifiedDate(petition.getModifiedDate())
                .build();
    }

    public static PetitionListResponseDto secretPetition(Petition petition){
        return PetitionListResponseDto.builder()
                .id(petition.getId())
                .title("비밀청원입니다.")
                .like(petition.getGood())
                .view(petition.getView())
                .imageCount(petition.getImageCount())
                .createDate(petition.getCreateDate())
                .modifiedDate(petition.getModifiedDate())
                .build();
    }
}
