package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,"동일한 이메일이 존재합니다."),
    ID_NOT_FOUND(HttpStatus.UNAUTHORIZED,"아이디가 존재하지 않습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.UNAUTHORIZED,"비밀번호가 틀립니다.");


    private final HttpStatus status;
    private final String message;
}
