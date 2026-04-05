package kr.inuappcenterportal.inuportal.domain.directory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DirectorySourceTemplateType {

    SUBVIEW_DO("서브뷰 페이지"),
    INDEX_DO("인덱스 페이지"),
    BOARD_PHP("게시판 페이지"),
    OTHER("기타");

    private final String label;

    public static DirectorySourceTemplateType fromUrl(String url) {
        if (url == null || url.isBlank()) {
            return OTHER;
        }
        if (url.contains("board.php")) {
            return BOARD_PHP;
        }
        if (url.contains("subview.do")) {
            return SUBVIEW_DO;
        }
        if (url.contains("index.do")) {
            return INDEX_DO;
        }
        return OTHER;
    }
}
