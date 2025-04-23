package kr.inuappcenterportal.inuportal.firebase;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class FirebaseRepositoryTest {
    @Autowired
    FcmTokenRepository fcmTokenRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("관리자들의 토큰을 호출합니다.")
    public void getAdminTokenTest(){
        List<Member> members = new ArrayList<>();
        Member member1 = Member.builder().studentId("201900001").roles(List.of("ROLE_ADMIN")).build();
        Member member2 = Member.builder().studentId("201900002").roles(List.of("ROLE_ADMIN")).build();
        Member member3 = Member.builder().studentId("201900003").roles(List.of("ROLE_USER")).build();
        members.add(Member.builder().studentId("201900004").roles(List.of("ROLE_USER")).build());
        members.add(Member.builder().studentId("201900005").roles(List.of("ROLE_ADMIN")).build());
        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);
        memberRepository.saveAll(members);
        List<FcmToken> fcmTokens = new ArrayList<>();
        fcmTokens.add(FcmToken.builder().token("token1").memberId(member1.getId()).build());
        fcmTokens.add(FcmToken.builder().token("token2").memberId(null).build());
        fcmTokens.add(FcmToken.builder().token("token3").memberId(member2.getId()).build());
        fcmTokens.add(FcmToken.builder().token("token4").memberId(member3.getId()).build());
        fcmTokenRepository.saveAll(fcmTokens);
        List<String> adminToken = fcmTokenRepository.findAllAdminTokens();
        assertAll(
                ()->assertTrue(adminToken.contains("token1")),
                ()->assertTrue(adminToken.contains("token3")),
                ()->assertEquals(adminToken.size(),2)
        );
    }

    @Test
    @DisplayName("모든 토큰을 불러옵니다.")
    public void getAllToken(){
        List<FcmToken> fcmTokens = new ArrayList<>();
        fcmTokens.add(FcmToken.builder().token("token1").build());
        fcmTokens.add(FcmToken.builder().token("token2").build());
        fcmTokens.add(FcmToken.builder().token("token3").build());
        fcmTokens.add(FcmToken.builder().token("token4").build());
        fcmTokenRepository.saveAll(fcmTokens);
        List<String> tokens = fcmTokenRepository.findAllTokens();
        assertEquals(tokens.size(),fcmTokens.size());
    }
}
