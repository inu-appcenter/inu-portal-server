package kr.inuappcenterportal.inuportal.domain.search.event;

import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.search.service.SearchIndexSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostIndexEventListener {

    private final PostRepository postRepository;
    private final SearchIndexSyncService searchIndexSyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostIndexEvent(PostIndexEvent event) {
        log.debug("Received PostIndexEvent: postId={}, type={}", event.getPostId(), event.getEventType());
        try {
            switch (event.getEventType()) {
                case SAVE, UPDATE -> postRepository.findByIdAndIsDeletedFalse(event.getPostId())
                        .ifPresent(searchIndexSyncService::indexPost);
                case DELETE -> searchIndexSyncService.deletePost(event.getPostId());
            }
        } catch (Exception e) {
            log.error("Failed to process PostIndexEvent for postId {}: {}", event.getPostId(), e.getMessage());
        }
    }
}
