package kr.inuappcenterportal.inuportal.domain.course.crawler;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CourseOverviewParser {
    public List<CourseOverviewItemDto> parse(Document document) {
        Elements items = document.select("ul.hBox6 > li");

        List<CourseOverviewItemDto> courseOverviewItems = new ArrayList<>();

        for (Element item : items) {
            String title = item.select(".tit1").text();
            Element titleElement = item.selectFirst(".tit");

            if (title.isBlank() && titleElement != null) {
                title = titleElement.ownText();
            }

            if (title.isBlank() && titleElement != null) {
                title = titleElement.text();
            }

            String content = item.select(".cont").text();

            if (title.isBlank() || content.isBlank()) {
                continue;
            }

            courseOverviewItems.add(new CourseOverviewItemDto(title, content));
        }

        return courseOverviewItems;
    }
}
