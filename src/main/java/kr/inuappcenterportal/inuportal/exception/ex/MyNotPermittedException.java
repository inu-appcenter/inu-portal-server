package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyNotPermittedException extends RuntimeException {
    private MyErrorCode errorCode;
    public MyNotPermittedException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
