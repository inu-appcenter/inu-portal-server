package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramScriptPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecondDormitoryInstagramOutputReader {

    private final ObjectMapper objectMapper;

    public List<InstagramMenuPost> readHistoryPosts(Path historyFile) throws IOException {
        if (!Files.exists(historyFile)) {
            return List.of();
        }

        List<InstagramScriptPost> rawPosts = objectMapper.readValue(
                historyFile.toFile(),
                new TypeReference<>() {
                }
        );

        List<InstagramMenuPost> posts = new ArrayList<>();
        for (InstagramScriptPost rawPost : rawPosts) {
            OffsetDateTime publishedAt = parsePublishedAt(rawPost.publishedAtIso());
            posts.add(new InstagramMenuPost(rawPost.postUrl(), rawPost.caption(), publishedAt));
        }
        return posts;
    }

    private OffsetDateTime parsePublishedAt(String publishedAtIso) {
        if (publishedAtIso == null || publishedAtIso.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(publishedAtIso.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
