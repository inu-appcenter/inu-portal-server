package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.postLike.model.PostLike;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "student_id")
    private String studentId;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(name = "fire_id")
    private Long fireId;

    @Column(name = "terms_agreed")
    private Boolean termsAgreed;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "profile_modified_at")
    private LocalDateTime profileModifiedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL)
    private List<Scrap> scraps;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL)
    private List<Folder> folders;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.PERSIST)
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.PERSIST)
    private List<Post> posts;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.PERSIST)
    private List<Reply> replies;

    @Builder
    public Member(String studentId, List<String> roles) {
        this.studentId = studentId;
        this.nickname = studentId;
        this.roles = roles;
        this.fireId = 1L;
        this.termsAgreed = false;
    }

    public void updateNicknameAndFire(String nickname, Long fireId) {
        this.nickname = nickname;
        this.fireId = fireId;
        touchProfileModifiedAt();
    }

    public void updateNickName(String nickname) {
        this.nickname = nickname;
        touchProfileModifiedAt();
    }

    public void updateFire(Long fireId) {
        this.fireId = fireId;
        touchProfileModifiedAt();
    }

    public void updateDepartment(Department department) {
        this.department = department;
        touchProfileModifiedAt();
    }

    public void agreeTerms() {
        this.termsAgreed = true;
        touchProfileModifiedAt();
    }

    public void updateRoles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
    }

    public void updateLastSeenAt() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public boolean shouldUpdateLastSeenAt(LocalDateTime now, long thresholdMinutes) {
        if (this.lastSeenAt == null) {
            return true;
        }

        return !this.lastSeenAt.isAfter(now.minusMinutes(thresholdMinutes));
    }

    @PrePersist
    private void initializeTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.profileModifiedAt = now;
    }

    private void touchProfileModifiedAt() {
        this.profileModifiedAt = LocalDateTime.now();
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles")
    private List<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
