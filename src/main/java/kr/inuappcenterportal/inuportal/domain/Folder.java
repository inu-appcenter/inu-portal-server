package kr.inuappcenterportal.inuportal.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "folder")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "folder",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<FolderPost> folderPosts;

    @Builder
    public Folder(String name, Member member){
        this.name = name;
        this.member = member;
    }

    public void update(String name){
        this.name = name;
    }
}
