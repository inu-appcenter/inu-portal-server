package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,"동일한 이메일이 존재합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 게시글입니다."),
    ID_NOT_FOUND(HttpStatus.UNAUTHORIZED,"아이디가 존재하지 않습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.UNAUTHORIZED,"비밀번호가 틀립니다."),
    HAS_NOT_AUTHORIZATION(HttpStatus.FORBIDDEN,"이 게시글의 수정/삭제에 대한 권한이 없습니다."),
    NOT_MULTIPLE_LIKE(HttpStatus.CONFLICT,"이미 좋아요를 눌렀습니다."),
    NOT_MULTIPLE_DISLIKE(HttpStatus.CONFLICT,"이미 싫어요를 눌렀습니다.");


    private final HttpStatus status;
    private final String message;
}
