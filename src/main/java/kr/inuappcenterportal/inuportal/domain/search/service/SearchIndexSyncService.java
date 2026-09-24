package kr.inuappcenterportal.inuportal.domain.search.service;

import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import kr.inuappcenterportal.inuportal.domain.club.repository.ClubRepository;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.repository.DirectoryEntryRepository;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.search.document.*;
import kr.inuappcenterportal.inuportal.domain.search.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexSyncService {

    private final NoticeRepository noticeRepository;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final PostRepository postRepository;
    private final ScheduleRepository scheduleRepository;
    private final DirectoryEntryRepository directoryEntryRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final ClubRepository clubRepository;

    private final NoticeSearchRepository noticeSearchRepository;
    private final DepartmentNoticeSearchRepository departmentNoticeSearchRepository;
    private final PostSearchRepository postSearchRepository;
    private final ScheduleSearchRepository scheduleSearchRepository;
    private final DirectorySearchRepository directorySearchRepository;
    private final CourseSearchRepository courseSearchRepository;
    private final ClubSearchRepository clubSearchRepository;

    @Transactional(readOnly = true)
    public int syncAll() {
        log.info("Starting full re-indexing of all domains to Elasticsearch...");
        int total = 0;
        total += syncNotices();
        total += syncDepartmentNotices();
        total += syncPosts();
        total += syncSchedules();
        total += syncDirectory();
        total += syncCourses();
        total += syncClubs();
        log.info("Full re-indexing completed successfully. Total indexed documents: {}", total);
        return total;
    }

    private static final int BATCH_SIZE = 300;

    private <T> void saveInBatches(List<T> items, java.util.function.Consumer<List<T>> batchConsumer) {
        if (items == null || items.isEmpty()) return;
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            batchConsumer.accept(items.subList(i, end));
        }
    }

    @Transactional(readOnly = true)
    public int syncNotices() {
        List<Notice> notices = noticeRepository.findAll();
        List<NoticeDocument> docs = notices.stream().map(this::toNoticeDocument).collect(Collectors.toList());
        saveInBatches(docs, noticeSearchRepository::saveAll);
        log.info("Indexed {} notices in batches of {}", docs.size(), BATCH_SIZE);
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncDepartmentNotices() {
        List<DepartmentNotice> deptNotices = departmentNoticeRepository.findAll();
        List<DepartmentNoticeDocument> docs = deptNotices.stream().map(this::toDepartmentNoticeDocument).collect(Collectors.toList());
        saveInBatches(docs, departmentNoticeSearchRepository::saveAll);
        log.info("Indexed {} department notices in batches of {}", docs.size(), BATCH_SIZE);
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncPosts() {
        List<Post> posts = postRepository.findAllByIsDeletedFalse();
        List<PostDocument> docs = posts.stream().map(this::toPostDocument).collect(Collectors.toList());
        saveInBatches(docs, postSearchRepository::saveAll);
        log.info("Indexed {} posts in batches of {}", docs.size(), BATCH_SIZE);
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncSchedules() {
        List<Schedule> schedules = scheduleRepository.findAll();
        List<ScheduleDocument> docs = schedules.stream().map(s -> ScheduleDocument.builder()
                .id(s.getId())
                .content(s.getContent())
                .startDate(s.getStartDate() != null ? s.getStartDate().toString() : null)
                .endDate(s.getEndDate() != null ? s.getEndDate().toString() : null)
                .department(s.getDepartment() != null ? s.getDepartment().name() : null)
                .aiGenerated(s.getAiGenerated())
                .build()
        ).collect(Collectors.toList());

        saveInBatches(docs, scheduleSearchRepository::saveAll);
        log.info("Indexed {} schedules", docs.size());
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncDirectory() {
        List<DirectoryEntry> entries = directoryEntryRepository.findAll();
        List<DirectoryDocument> docs = entries.stream().map(e -> DirectoryDocument.builder()
                .id(e.getId())
                .name(e.getName())
                .affiliation(e.getAffiliation())
                .detailAffiliation(e.getDetailAffiliation())
                .position(e.getPosition())
                .duties(e.getDuties())
                .email(e.getEmail())
                .phoneNumber(e.getPhoneNumber())
                .build()
        ).collect(Collectors.toList());

        saveInBatches(docs, directorySearchRepository::saveAll);
        log.info("Indexed {} directory entries", docs.size());
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncCourses() {
        List<CourseOffering> offerings = courseOfferingRepository.findAll();
        List<CourseDocument> docs = offerings.stream().map(co -> CourseDocument.builder()
                .id(co.getId())
                .subjectNumber(co.getSubjectNumber())
                .title(co.getCourse() != null ? co.getCourse().getTitle() : null)
                .englishTitle(co.getCourse() != null ? co.getCourse().getEnglishTitle() : null)
                .professor(co.getProfessor())
                .credit(co.getCredit())
                .hyName(co.getHyNameRaw())
                .isuName(co.getIsuNameRaw())
                .build()
        ).collect(Collectors.toList());

        saveInBatches(docs, courseSearchRepository::saveAll);
        log.info("Indexed {} courses", docs.size());
        return docs.size();
    }

    @Transactional(readOnly = true)
    public int syncClubs() {
        List<Club> clubs = clubRepository.findAll();
        List<ClubDocument> docs = clubs.stream().map(c -> ClubDocument.builder()
                .id(c.getId())
                .name(c.getName())
                .category(c.getCategory())
                .recruitContent(c.getRecruit())
                .build()
        ).collect(Collectors.toList());

        saveInBatches(docs, clubSearchRepository::saveAll);
        log.info("Indexed {} clubs", docs.size());
        return docs.size();
    }

    // 단건 인덱싱 / 업데이트 / 삭제
    @Async
    public void indexPost(Post post) {
        try {
            postSearchRepository.save(toPostDocument(post));
        } catch (Exception e) {
            log.warn("Failed to index post in elasticsearch: {}", e.getMessage());
        }
    }

    @Async
    public void deletePost(Long postId) {
        try {
            postSearchRepository.deleteById(postId);
        } catch (Exception e) {
            log.warn("Failed to delete post from elasticsearch: {}", e.getMessage());
        }
    }

    @Async
    public void indexNotice(Notice notice) {
        try {
            noticeSearchRepository.save(toNoticeDocument(notice));
        } catch (Exception e) {
            log.warn("Failed to index notice in elasticsearch: {}", e.getMessage());
        }
    }

    @Async
    public void deleteNotice(Long noticeId) {
        try {
            noticeSearchRepository.deleteById(noticeId);
        } catch (Exception e) {
            log.warn("Failed to delete notice from elasticsearch: {}", e.getMessage());
        }
    }

    @Async
    public void indexDepartmentNotice(DepartmentNotice deptNotice) {
        try {
            departmentNoticeSearchRepository.save(toDepartmentNoticeDocument(deptNotice));
        } catch (Exception e) {
            log.warn("Failed to index department notice in elasticsearch: {}", e.getMessage());
        }
    }

    public NoticeDocument toNoticeDocument(Notice n) {
        String contentText = (n.getContent() != null && n.getContent().getContentText() != null)
                ? n.getContent().getContentText()
                : n.getDescription();
        return NoticeDocument.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(contentText)
                .category(n.getCategory())
                .writer(n.getWriter())
                .url(n.getUrl())
                .createDate(n.getCreateDate())
                .build();
    }

    public DepartmentNoticeDocument toDepartmentNoticeDocument(DepartmentNotice dn) {
        StringBuilder sb = new StringBuilder();
        if (dn.getContent() != null) {
            if (dn.getContent().getContentText() != null) {
                sb.append(dn.getContent().getContentText()).append(" ");
            }
            if (dn.getContent().getOcrText() != null) {
                sb.append(dn.getContent().getOcrText()).append(" ");
            }
            if (dn.getContent().getAttachmentText() != null) {
                sb.append(dn.getContent().getAttachmentText());
            }
        }
        String deptName = (dn.getDepartment() != null) ? dn.getDepartment().getDepartmentName() : "";
        return DepartmentNoticeDocument.builder()
                .id(dn.getId())
                .department(dn.getDepartment() != null ? dn.getDepartment().name() : null)
                .departmentName(deptName)
                .title(dn.getTitle())
                .content(sb.toString().trim())
                .writer(null)
                .url(dn.getUrl())
                .createDate(dn.getCreateDate() != null ? dn.getCreateDate().toString() : null)
                .build();
    }

    public PostDocument toPostDocument(Post p) {
        return PostDocument.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .category(p.getCategory())
                .writer(p.getAnonymous() != null && p.getAnonymous() ? "익명" : (p.getMember() != null ? p.getMember().getNickname() : "알수없음"))
                .good(p.getGood() != null ? p.getGood().intValue() : 0)
                .scrap(p.getScrap() != null ? p.getScrap().intValue() : 0)
                .createDate(p.getCreateDate() != null ? p.getCreateDate().toString() : null)
                .build();
    }
}
