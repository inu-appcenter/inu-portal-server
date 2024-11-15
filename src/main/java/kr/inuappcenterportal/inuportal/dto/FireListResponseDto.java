package kr.inuappcenterportal.inuportal.dto;


import kr.inuappcenterportal.inuportal.domain.Fire;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FireListResponseDto {

    private String request_id;
    private String prompt;

    @Builder
    private FireListResponseDto(String request_id, String prompt){
        this.request_id = request_id;
        this.prompt = prompt;
    }

    public static FireListResponseDto of(Fire fire){
        return FireListResponseDto.builder().request_id(fire.getRequestId()).prompt(fire.getPrompt()).build();
    }
}
