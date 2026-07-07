package kr.inuappcenterportal.inuportal.domain.course.crawler;

import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CurriculumParser {
    private static final int REQUIRED_CELL_COUNT = 5;

    public List<CurriculumItemDto> parse(Document document) {
        Elements rows = document.select(".func-table table tbody tr");

        List<CurriculumItemDto> curriculumItems = new ArrayList<>();

        for (Element row : rows) {
            Elements cells = row.select("th, td");

            if (cells.size() < REQUIRED_CELL_COUNT) {
                log.debug("교육과정 행 파싱 스킵: cellCount={}, row={}", cells.size(), row.text());
                continue;
            }

            String targetGrade = cells.get(0).text();
            String targetTerm = cells.get(1).text();
            String completionDivision = cells.get(2).text();
            String title = cells.get(3).text();
            String credit = cells.get(4).text();

            if (targetGrade.isBlank()
                    || targetTerm.isBlank()
                    || completionDivision.isBlank()
                    || title.isBlank()
                    || credit.isBlank()) {
                continue;
            }
            
            curriculumItems.add(new CurriculumItemDto(
                    targetGrade,
                    targetTerm,
                    completionDivision,
                    title,
                    credit
            ));
        }
        return curriculumItems;
    }
}
