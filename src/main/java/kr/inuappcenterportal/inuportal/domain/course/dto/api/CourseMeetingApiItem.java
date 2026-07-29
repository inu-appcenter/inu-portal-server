package kr.inuappcenterportal.inuportal.domain.course.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourseMeetingApiItem(
        @JsonProperty("YEAR")
        String year,

        @JsonProperty("TERM_CODE")
        String termCode,

        @JsonProperty("TERM_NAME")
        String termName,

        @JsonProperty("HAKSU_CODE")
        String haksuCode,

        @JsonProperty("DAY_CODE")
        String dayCode,

        @JsonProperty("DAY_NAME")
        String dayName,

        @JsonProperty("LECTM_CODE")
        String lectmCode,

        @JsonProperty("LECTM_NAME")
        String lectmName,

        @JsonProperty("LECTM_START")
        String lectmStart,

        @JsonProperty("LECTM_END")
        String lectmEnd,

        @JsonProperty("ROOM_CODE")
        String roomCode,

        @JsonProperty("ROOM_NAME")
        String roomName,

        @JsonProperty("INPT_DATE")
        String inputDate,

        @JsonProperty("MOD_DATE")
        String modifiedDate

) {
}
