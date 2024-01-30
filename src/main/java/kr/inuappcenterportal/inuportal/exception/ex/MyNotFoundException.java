package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyNotFoundException extends RuntimeException{
    private MyErrorCode errorCode;

    public MyNotFoundException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
