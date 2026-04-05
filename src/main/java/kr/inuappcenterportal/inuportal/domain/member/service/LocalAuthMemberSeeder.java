package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.config.LocalAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(name = "app.local-auth.enabled", havingValue = "true")
public class LocalAuthMemberSeeder implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final LocalAuthProperties localAuthProperties;

    @Override
    public void run(ApplicationArguments args) {
        for (LocalAuthProperties.SeedUser seedUser : localAuthProperties.getSeedUsers()) {
            if (seedUser.getStudentId() == null || seedUser.getStudentId().isBlank()) {
                continue;
            }

            Member member = memberRepository.findByStudentId(seedUser.getStudentId())
                    .map(existingMember -> synchronizeRoles(existingMember, seedUser))
                    .orElseGet(() -> createSeedMember(seedUser));

            log.info("Local auth seed user ready. studentId:{}, roles:{}", member.getStudentId(), member.getRoles());
        }
    }

    private Member synchronizeRoles(Member member, LocalAuthProperties.SeedUser seedUser) {
        if (!member.getRoles().equals(seedUser.getResolvedRoles())) {
            member.updateRoles(seedUser.getResolvedRoles());
            return memberRepository.save(member);
        }
        return member;
    }

    private Member createSeedMember(LocalAuthProperties.SeedUser seedUser) {
        Member member = Member.builder()
                .studentId(seedUser.getStudentId())
                .roles(seedUser.getResolvedRoles())
                .build();
        return memberRepository.save(member);
    }
}
