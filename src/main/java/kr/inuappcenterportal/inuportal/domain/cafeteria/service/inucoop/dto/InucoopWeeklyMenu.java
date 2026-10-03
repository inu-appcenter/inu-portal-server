package kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto;

import java.util.List;
import java.util.Optional;

public record InucoopWeeklyMenu(String weekRange, List<InucoopMenuRow> rows) {

    public static InucoopWeeklyMenu empty() {
        return new InucoopWeeklyMenu(null, List.of());
    }

    public Optional<InucoopMenuRow> findRow(String label) {
        return rows.stream()
                .filter(row -> row.label().equals(label))
                .findFirst();
    }

    public List<String> labels() {
        return rows.stream().map(InucoopMenuRow::label).toList();
    }
}
