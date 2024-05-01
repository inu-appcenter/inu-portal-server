package kr.inuappcenterportal.inuportal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FireResponseDto {
    private List<String> data;
    private boolean is_generating;
    private String duration;
    private String average_duration;

}
