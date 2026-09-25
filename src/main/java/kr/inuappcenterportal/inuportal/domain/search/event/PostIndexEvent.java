package kr.inuappcenterportal.inuportal.domain.search.event;

import lombok.Getter;

@Getter
public class PostIndexEvent {

    public enum EventType {
        SAVE, UPDATE, DELETE
    }

    private final Long postId;
    private final EventType eventType;

    public PostIndexEvent(Long postId, EventType eventType) {
        this.postId = postId;
        this.eventType = eventType;
    }

    public static PostIndexEvent save(Long postId) {
        return new PostIndexEvent(postId, EventType.SAVE);
    }

    public static PostIndexEvent update(Long postId) {
        return new PostIndexEvent(postId, EventType.UPDATE);
    }

    public static PostIndexEvent delete(Long postId) {
        return new PostIndexEvent(postId, EventType.DELETE);
    }
}
