package kr.inuappcenterportal.inuportal.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ResponseDto<T> {
    private final T data;
    private final String msg;

    @Builder
    private ResponseDto(T data, String msg){
        this.data =data;
        this.msg = msg;
    }

    public static <T> ResponseDto<T> of(T data, String msg){
        return ResponseDto.<T>builder().data(data).msg(msg).build();
    }


}
