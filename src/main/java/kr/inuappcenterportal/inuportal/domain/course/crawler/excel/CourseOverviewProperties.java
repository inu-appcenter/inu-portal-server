package kr.inuappcenterportal.inuportal.domain.course.crawler.excel;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "course.overview")
public record CourseOverviewProperties(
        Path directory,
        String filenamePattern,
        Path capacityDirectory,
        String capacityFilenamePattern
) {
    private static final Path DEFAULT_DIRECTORY = Path.of("/home/serverking/intip/overviews");
    private static final String DEFAULT_FILENAME_PATTERN = "course-offering-{year}-{term-code}.xlsx";
    private static final String DEFAULT_CAPACITY_FILENAME_PATTERN = "course-offering-capacity-{year}-{term-code}.xlsx";

    public CourseOverviewProperties {
        if (directory == null) {
            directory = DEFAULT_DIRECTORY;
        }
        if (filenamePattern == null || filenamePattern.isBlank()) {
            filenamePattern = DEFAULT_FILENAME_PATTERN;
        }
        if (capacityDirectory == null) {
            capacityDirectory = directory;
        }
        if (capacityFilenamePattern == null || capacityFilenamePattern.isBlank()) {
            capacityFilenamePattern = DEFAULT_CAPACITY_FILENAME_PATTERN;
        }
    }

    public Path resolvePath(int year, SemesterTerm term) {
        return directory.resolve(resolveFilename(filenamePattern, year, term));
    }

    public Path resolveCapacityPath(int year, SemesterTerm term) {
        return capacityDirectory.resolve(resolveFilename(capacityFilenamePattern, year, term));
    }

    private String resolveFilename(String pattern, int year, SemesterTerm term) {
        return pattern
                .replace("{year}", String.valueOf(year))
                .replace("{term}", term.name().toLowerCase())
                .replace("{term-code}", termCode(term));
    }

    private String termCode(SemesterTerm term) {
        return switch (term) {
            case FIRST -> "10";
            case SECOND -> "20";
            case SUMMER -> "30";
            case WINTER -> "40";
        };
    }
}
