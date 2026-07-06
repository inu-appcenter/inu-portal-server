package kr.inuappcenterportal.inuportal.domain.course.crawler;

import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CurriculumParser {
    public List<CurriculumItemDto> parse(Document document) {
        Elements rows = document.select(".func-table table tbody tr");

        List<CurriculumItemDto> curriculumItems = new ArrayList<>();

        for (Element row : rows) {
            Elements cells = row.select("th, td");

            String targetGrade = cells.get(0).text();
            String targetTerm = cells.get(1).text();
            String completionDivision = cells.get(2).text();
            String title = cells.get(3).text();
            String credit = cells.get(4).text();

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
