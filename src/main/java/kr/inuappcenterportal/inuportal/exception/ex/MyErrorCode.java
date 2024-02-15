package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    POST_SCRAP_LIST_NOT_FOUND(HttpStatus.NOT_FOUND,"게시글 리스트가 비어있습니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND,"스크랩하지 않은 게시물입니다."),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 스크랩폴더입니다."),
    FOLDER_OR_POST_NOT_FOUND(HttpStatus.NOT_FOUND,"스크랩폴더나 게시글이 존재하지 않습니다."),
    USER_OR_POST_NOT_FOUND(HttpStatus.NOT_FOUND,"유저나 게시글이 존재하지 않습니다."),
    USER_OR_REPLY_NOT_FOUND(HttpStatus.NOT_FOUND,"유저나 댓글이 존재하지 않습니다."),
    NOT_REPLY_ON_REREPLY(HttpStatus.BAD_REQUEST,"대댓글에 댓글을 작성할 수 없습니다."),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 댓글입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 카테고리입니다."),
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,"동일한 이메일이 존재합니다."),
    POST_DUPLICATE_FOLDER(HttpStatus.BAD_REQUEST,"스크랩폴더에 존재하는 게시글입니다."),
    USER_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST,"동일한 닉네임이 존재합니다."),
    USER_DUPLICATE_CATEGORY(HttpStatus.BAD_REQUEST,"동일한 카테고리가 존재합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 게시글입니다."),
    ID_NOT_FOUND(HttpStatus.UNAUTHORIZED,"아이디가 존재하지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,"토큰의 값이 존재하지않습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.UNAUTHORIZED,"비밀번호가 틀립니다."),
    HAS_NOT_POST_AUTHORIZATION(HttpStatus.FORBIDDEN,"이 게시글의 수정/삭제에 대한 권한이 없습니다."),
    HAS_NOT_REPLY_AUTHORIZATION(HttpStatus.FORBIDDEN,"이 게시글의 수정/삭제에 대한 권한이 없습니다."),
    NOT_MULTIPLE_LIKE(HttpStatus.CONFLICT,"이미 좋아요를 눌렀습니다."),
    NOT_MULTIPLE_DISLIKE(HttpStatus.CONFLICT,"이미 싫어요를 눌렀습니다.");


    private final HttpStatus status;
    private final String message;
}
