package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,name = "student_id")
    private String studentId;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "fire_id")
    private Long fireId;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<Scrap> scraps;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<Folder> folders;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    private List<Post> posts;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    private List<Reply> replies;

    @Builder
    public Member(String studentId, String nickname, List<String> roles){
        this.studentId = studentId;
        this.nickname = nickname;
        this.roles = roles;
        this.fireId = 1L;
    }

    public void updateNicknameAndFire(String nickname,Long fireId){
        this.nickname = nickname;
        this.fireId = fireId;
    }
    public void updateNickName(String nickname){
        this.nickname = nickname;
    }
    public void updateFire(Long fireId){
        this.fireId = fireId;
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
