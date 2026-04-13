package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cafeteria.instagram.second-dormitory")
public class SecondDormitoryInstagramProperties {

    private boolean enabled = true;
    private String username = "incheondae_2gisik";
    private int recentCount = 6;
    private String zone = "Asia/Seoul";
    private String pythonCommand = "python3";
    private String scriptDir = "instagram-2gisik-scrapping";
    private String scriptFile = "insta_latest_post.py";
    private String outputFile = "output/latest_post.json";
    private String historyOutputFile = "output/scraped_posts.json";
    private String profileDir = "chrome-profile";
    private int timeoutSeconds = 180;

    public int getResolvedRecentCount() {
        return Math.max(recentCount, 1);
    }

    public int getResolvedTimeoutSeconds() {
        return Math.max(timeoutSeconds, 30);
    }

    public ZoneId resolveZoneId() {
        return ZoneId.of(zone);
    }

    public Path resolveScriptDir() {
        return Paths.get(scriptDir.trim());
    }

    public Path resolveScriptFile() {
        return resolveScriptDir().resolve(scriptFile.trim());
    }

    public Path resolveOutputFile() {
        return resolveScriptDir().resolve(outputFile.trim());
    }

    public Path resolveHistoryOutputFile() {
        return resolveScriptDir().resolve(historyOutputFile.trim());
    }

    public Path resolveProfileDir() {
        return resolveScriptDir().resolve(profileDir.trim());
    }
}
