package kr.inuappcenterportal.inuportal.domain.petition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "총학생회 청원 응답Dto")
@Getter
@NoArgsConstructor
public class PetitionResponseDto {
    @Schema(description = "총학생회 청원 데이터베이스 아이디값")
    private Long id;

    @Schema(description = "제목",example = "제목")
    private String title;

    @Schema(description = "내용", example = "내용")
    private String content;

    @Schema(description = "작성자", example = "201901234")
    private String writer;

    @Schema(description = "조회수")
    private Long view;

    @Schema(description = "좋아요")
    private Long like;

    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private String createDate;

    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private String modifiedDate;

    @Schema(description = "이미지 갯수")
    private Long imageCount;

    @Schema(description = "수정/삭제 가능 여부")
    private Boolean hasAuthority;

    @Schema(description = "좋아요 여부",example = "false")
    private Boolean isLiked;

    @Builder
    private PetitionResponseDto(Long id, String title, String content, Long view, Long like, String createDate, String modifiedDate, Long imageCount, Boolean hasAuthority, Boolean isLiked, String writer){
        this.id = id;
        this.title = title;
        this.content = content;
        this.view = view;
        this.like = like;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.imageCount = imageCount;
        this.hasAuthority = hasAuthority;
        this.isLiked = isLiked;
        this.writer = writer;
    }

    public static PetitionResponseDto of(Petition petition, boolean hasAuthority, boolean isLiked,String writer){
        return PetitionResponseDto.builder()
                .id(petition.getId())
                .title(petition.getTitle())
                .content(petition.getContent())
                .view(petition.getView())
                .like(petition.getGood())
                .createDate(petition.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .modifiedDate(petition.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .imageCount(petition.getImageCount())
                .hasAuthority(hasAuthority)
                .isLiked(isLiked)
                .writer(writer)
                .build();
    }


}
