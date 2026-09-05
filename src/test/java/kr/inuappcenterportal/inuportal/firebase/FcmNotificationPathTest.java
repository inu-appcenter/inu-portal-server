package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FcmNotificationPathTest {

    @MockBean
    WeatherService weatherService;

    @MockBean
    ScheduleService scheduleService;

    @MockBean
    NoticeService noticeService;

    @MockBean
    CafeteriaService cafeteriaService;

    @MockBean
    ImageService imageService;

    @MockBean
    SchoolLoginRepository schoolLoginRepository;

    @MockBean
    RedisService redisService;

    @MockBean
    FirebaseMessaging firebaseMessaging;

    @Autowired
    FcmService fcmService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    FcmMessageRepository fcmMessageRepository;

    @Autowired
    MemberFcmMessageRepository memberFcmMessageRepository;

    private Member saveMember(String studentId) {
        return memberRepository.save(Member.builder()
                .studentId(studentId)
                .roles(Collections.singletonList("ROLE_USER"))
                .build());
    }

    @Test
    @DisplayName("알림 조회 시 발송 payload의 path가 그대로 내려간다. path 없이 발송된 알림은 null이다.")
    void notificationPathTest() {
        Member member = saveMember("202000010");

        FcmMessage withPath = fcmMessageRepository.save(FcmMessage.builder()
                .title("댓글 알림")
                .body("새 댓글이 달렸습니다.")
                .targetId(12L)
                .path("/home/tips/12")
                .build());
        FcmMessage withoutPath = fcmMessageRepository.save(FcmMessage.builder()
                .title("공지")
                .body("path 없이 발송된 과거 알림")
                .build());

        memberFcmMessageRepository.save(MemberFcmMessage.of(withPath.getId(), member.getId(), FcmMessageType.POST_REPLY));
        memberFcmMessageRepository.save(MemberFcmMessage.of(withoutPath.getId(), member.getId(), FcmMessageType.GENERAL));

        ListResponseDto<NotificationResponse> result = fcmService.findNotifications(member, 1);
        List<NotificationResponse> contents = result.getContents();

        // 최신순(id DESC)이므로 나중에 저장한 path 없는 알림이 앞에 온다.
        assertEquals(2, contents.size());
        assertNull(contents.get(0).path());
        assertEquals("/home/tips/12", contents.get(1).path());
    }

    @Test
    @DisplayName("키워드 공지 알림도 발송 payload와 같은 path를 이력에 저장한다.")
    void keywordNoticePathIsPersistedTest() {
        Member member = saveMember("202000011");
        String noticeUrl = "https://www.inu.ac.kr/bbs/inu/246/388671/artclView.do";

        Long fcmMessageId = fcmService.prepareKeywordNotice(
                Map.of("fcm-token-1", member.getId()),
                "키워드 알림",
                "공지 제목",
                FcmMessageType.SCHOOL_NOTICE,
                388671L,
                noticeUrl
        );

        FcmMessage saved = fcmMessageRepository.findById(fcmMessageId).orElseThrow();
        assertEquals(noticeUrl, saved.getPath());
    }
}
