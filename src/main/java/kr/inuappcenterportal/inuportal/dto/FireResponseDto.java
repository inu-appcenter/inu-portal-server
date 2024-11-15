package kr.inuappcenterportal.inuportal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FireResponseDto {
    private String status;
    private String request_id;
    private Integer request_head;
    private Integer eta;

    public void setTimePlus2(){
        this.eta +=2;
    }


}
