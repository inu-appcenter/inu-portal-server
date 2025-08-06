package kr.inuappcenterportal.inuportal.post;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class PostRepositoryTest {
    @Autowired
    PostRepository postRepository;
    @Autowired
    MemberRepository memberRepository;


    @Test
    @DisplayName("삭제되지 않은 게시글을 가져옵니다.")
    public void getPostByIdTest(){
        Member member1 = saveMember("20190000");
        Post post1 = Post.builder().member(member1).title("title1").content("content1").category("수강신청").build();
        postRepository.save(post1);
        Optional<Post> post = postRepository.findByIdAndIsDeletedFalseWithPostMember(post1.getId());
        assertEquals(post.get().getId(),post1.getId());
    }

    @Test
    @DisplayName("삭제된 게시글을 가져옵니다.")
    public void getDeletedPostByIdTest(){
        Member member1 = saveMember("20190000");
        Post post1 = Post.builder().member(member1).title("title1").content("content1").category("수강신청").build();
        postRepository.save(post1);
        post1.delete();
        Optional<Post> post = postRepository.findByIdAndIsDeletedFalseWithPostMember(post1.getId());
        assertEquals(Optional.empty(),post);
    }

    @Test
    @DisplayName("게시글의 리스트를 가져옵니다 - 전체 게시글, 첫페이지, 차단 목록 없음")
    public void getPostListTest1(){
        Member member1 = saveMember("20190000");
        Post post1 = Post.builder().member(member1).title("title1").content("content1").category("수강신청").build();
        Post post2 = Post.builder().member(member1).title("title2").content("content2").category("수강신청").build();
        Post post3 = Post.builder().member(member1).title("title3").content("content3").category("수강신청").build();
        Post post4 = Post.builder().member(member1).title("title4").content("content4").category("수강신청").build();
        Post post5 = Post.builder().member(member1).title("title5").content("content5").category("수강신청").build();
        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));
        post5.delete();
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        List<Post> posts = postRepository.findFilteredPosts(null,null,null,pageable);
        assertAll(
                ()->assertEquals(posts.size(),4),
                ()->assertEquals(posts.get(0).getId(),post4.getId()),
                ()->assertEquals(posts.get(1).getId(),post3.getId()),
                ()->assertEquals(posts.get(2).getId(),post2.getId()),
                ()->assertEquals(posts.get(3).getId(),post1.getId())
        );
    }

    @Test
    @DisplayName("게시글의 리스트를 가져옵니다 -  수강신청 카테고리, 첫페이지, 차단 목록 있음")
    public void getPostListTest2(){
        Member member1 = saveMember("20190000");
        Post post1 = Post.builder().member(member1).title("title1").content("content1").category("수강신청").build();
        Post post2 = Post.builder().member(member1).title("title2").content("content2").category("학사").build();
        Post post3 = Post.builder().member(member1).title("title3").content("content3").category("학사").build();
        Post post4 = Post.builder().member(member1).title("title4").content("content4").category("수강신청").build();
        Post post5 = Post.builder().member(member1).title("title5").content("content5").category("수강신청").build();
        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));
        post5.delete();
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        List<Post> posts = postRepository.findFilteredPosts("수강신청",null,List.of(member1.getId()),pageable);
        assertAll(
                ()->assertEquals(posts.size(),1),
                ()->assertEquals(posts.get(0).getId(),post4.getId())
        );
    }

    private Member saveMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }
}
