package kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto;

import java.util.List;

/**
 * 생협 주간식단표의 끼니 한 줄. menus는 월요일부터 일요일까지 7개.
 */
public record InucoopMenuRow(String label, List<String> menus) {

    public String menuOf(int day) {
        return menus.get(day - 1);
    }
}
