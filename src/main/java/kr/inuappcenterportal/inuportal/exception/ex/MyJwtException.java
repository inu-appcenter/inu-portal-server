package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyJwtException extends  RuntimeException{
    private MyErrorCode errorCode;
    public MyJwtException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
