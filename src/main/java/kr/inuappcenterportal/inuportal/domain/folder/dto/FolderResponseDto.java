package kr.inuappcenterportal.inuportal.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "스크랩폴더 응답Dto")
@Getter
@NoArgsConstructor
public class FolderResponseDto {
    @Schema(description = "스크랩폴더의 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "스크랩폴더명",example = "나만의 꿀팁 폴더")
    private String name;

    @Builder
    private FolderResponseDto(Folder folder){
        this.id = folder.getId();
        this.name = folder.getName();
    }

    public static FolderResponseDto of(Folder folder){
        return FolderResponseDto.builder().folder(folder).build();
    }
}
