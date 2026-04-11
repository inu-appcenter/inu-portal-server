package kr.inuappcenterportal.inuportal.global.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class DepartmentCrawlConfig {

    private String titleSelector;
    private String dateSelector;
    private String linkSelector;
    private String viewsSelector;
    private Predicate<Element> skipCondition;
    private boolean useAbsoluteHref;
    private List<String> contentSelectors;
    private List<String> attachmentSelectors;
}
