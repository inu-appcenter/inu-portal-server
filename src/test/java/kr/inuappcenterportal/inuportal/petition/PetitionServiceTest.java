package kr.inuappcenterportal.inuportal.petition;

import kr.inuappcenterportal.inuportal.domain.petitionLike.model.PetitionLike;
import kr.inuappcenterportal.inuportal.domain.petitionLike.repository.PetitionLikeRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionListResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionRequestDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import kr.inuappcenterportal.inuportal.domain.petition.respoitory.PetitionRepository;
import kr.inuappcenterportal.inuportal.domain.petition.service.PetitionService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PetitionServiceTest {
    @InjectMocks
    PetitionService petitionService;

    @Mock
    PetitionRepository petitionRepository;

    @Mock
    PetitionLikeRepository petitionLikeRepository;

    @Mock
    ImageService imageService;

    @Mock
    RedisService redisService;

    @Test
    @DisplayName("총학생회 청원 저장 테스트")
    public void savePetitionTest() throws IOException {
        //given
        PetitionRequestDto petitionRequestDto = createPetition("제목","내용");
        Member member = createMember("20241234");
        List<MultipartFile> images = createDummyImages();
        Petition petition = createPetitionEntity("제목","내용",false,member);
        when(petitionRepository.save(any())).thenReturn(petition);

        //when
        petitionService.savePetition(petitionRequestDto,member,images);

        //then
        verify(petitionRepository, times(1)).save(any(Petition.class));
        verify(imageService,times(1)).saveImageWithThumbnail(any(),any(),any());
    }

    @Test
    @DisplayName("총학생회 청원 저장 테스트 - 비밀글")
    public void saveSecretPetitionTest() throws IOException {
        //given
        PetitionRequestDto petitionRequestDto = createPetition("제목","내용");
        Member member = createMember("20241234");
        List<MultipartFile> images = createDummyImages();
        Petition petition = createPetitionEntity("제목","내용",true,member);
        when(petitionRepository.save(any())).thenReturn(petition);

        //when
        petitionService.savePetition(petitionRequestDto,member,images);

        //then
        verify(petitionRepository, times(1)).save(any(Petition.class));
        verify(imageService,times(1)).saveImage(any(),any(),any());
    }


    @Test
    @DisplayName("총학생회 청원 이미지 가져오기 테스트")
    public void getPetitionImageTest(){
        //given
        Long petitionId = 1L;
        Long imageId = 1L;

        //when
        petitionService.getPetitionImage(petitionId,imageId);

        //then
        verify(imageService,times(1)).getImage(any(),any(),any());
    }
    @Test
    @DisplayName("총학생회 청원 수정 테스트")
    public void updatePetitionTest() throws IOException {
        //given
        Long petitionId = 1L;
        PetitionRequestDto petitionUpdateDto = createPetition("수정 제목","수정 내용");
        Member member = createMember("20241234");
        Petition petition = Petition.builder().title("제목").content("내용").member(member).build();
        List<MultipartFile> images = createDummyImages();
        when(petitionRepository.findByIdAndIsDeletedFalse(petitionId)).thenReturn(Optional.ofNullable(petition));

        //then
        Long id = petitionService.updatePetition(petitionId,petitionUpdateDto,member,images);

        //then
        assertEquals(id,petitionId);
        verify(petitionRepository,times(1)).findByIdAndIsDeletedFalse(petitionId);
        verify(imageService,times(1)).updateImages(any(),any(),any());
    }


    @Test
    @DisplayName("총학생회 청원 삭제 테스트")
    public void deletePetitionTest(){
        //given
        Long petitionId = 1L;
        Member member = createMember("20241234");
        Petition petition = Petition.builder().title("제목").content("내용").member(member).build();
        when(petitionRepository.findByIdAndIsDeletedFalse(petitionId)).thenReturn(Optional.ofNullable(petition));

        //when
        petitionService.deletePetition(petitionId,member);

        //then
        verify(petitionRepository,times(1)).findByIdAndIsDeletedFalse(petitionId);
    }

    @Test
    @DisplayName("총학생회 청원 좋아요 테스트 - 좋아요")
    public void likePetitionTest(){
        //given
        Long petitionId = 1L;
        Member petitionMember = createMember("20241234");
        Member member = createMember("20241111");
        ReflectionTestUtils.setField(member,"id",2L);
        Petition petition = Petition.builder().title("제목").content("내용").member(petitionMember).build();
        when(petitionRepository.findByIdWithLock(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(petitionLikeRepository.existsByMemberAndPetition(member,petition)).thenReturn(false);

        //when
        int num = petitionService.likePetition(petitionId,member);

        //then
        assertEquals(num,1);
        verify(petitionRepository,times(1)).findByIdWithLock(petitionId);
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(member,petition);
    }

    @Test
    @DisplayName("총학생회 청원 좋아요 테스트 - 좋아요 해제")
    public void unLikePetitionTest(){
        //given
        Long petitionId = 1L;
        Member petitionMember = createMember("20241234");
        Member member = createMember("20241111");
        ReflectionTestUtils.setField(member,"id",2L);
        Petition petition = Petition.builder().title("제목").content("내용").member(petitionMember).build();
        when(petitionRepository.findByIdWithLock(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(petitionLikeRepository.existsByMemberAndPetition(member,petition)).thenReturn(true);
        PetitionLike petitionLike = PetitionLike.builder().member(member).petition(petition).build();
        when(petitionLikeRepository.findPetitionLikeByMemberAndPetition(member,petition)).thenReturn(Optional.ofNullable(petitionLike));

        //when
        int num = petitionService.likePetition(petitionId,member);

        //then
        assertEquals(num,-1);
        verify(petitionRepository,times(1)).findByIdWithLock(petitionId);
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(member,petition);
        verify(petitionLikeRepository,times(1)).findPetitionLikeByMemberAndPetition(member,petition);
    }

    @Test
    @DisplayName("총학생회 청원 좋아요 실패 테스트 - 본인 청원에 대한 좋아요")
    public void likePetitionFailTest(){
        //given
        Long petitionId = 1L;
        Member petitionMember = createMember("20241234");
        Petition petition = Petition.builder().title("제목").content("내용").member(petitionMember).build();
        when(petitionRepository.findByIdWithLock(petitionId)).thenReturn(Optional.ofNullable(petition));

        //when
        MyException exception = assertThrows(MyException.class,()->petitionService.likePetition(petitionId,petitionMember));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"자신의 게시글에는 추천을 할 수 없습니다.");
        verify(petitionRepository,times(1)).findByIdWithLock(petitionId);
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 제 3자 청원")
    public void getPetitionTest(){
        //given
        Long petitionId = 1L;
        String address = "168.000.00.000";
        Member petitionMember = createMember("20241234");
        Petition petition = createPetitionEntity("제목","내용",false,petitionMember);
        when(petitionRepository.findByIdWithMember(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(redisService.isFirstConnect(address,petitionId,"petition")).thenReturn(true);
        when(petitionLikeRepository.existsByMemberAndPetition(any(),any())).thenReturn(false);

        //when
        PetitionResponseDto petitionResponseDto = petitionService.getPetition(petitionId,address,null);

        //then
        assertAll(
                ()->assertEquals(petitionResponseDto.getTitle(),"제목"),
                ()->assertEquals(petitionResponseDto.getContent(),"내용"),
                ()->assertEquals(petitionResponseDto.getHasAuthority(),false),
                ()->assertEquals(petitionResponseDto.getIsLiked(),false)
        );
        verify(petitionRepository,times(1)).findByIdWithMember(petitionId);
        verify(redisService,times(1)).isFirstConnect(any(),any(),any());
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(any(),any());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 본인의 청원 권한 확인")
    public void getPetitionTestMine(){
        //given
        Long petitionId = 1L;
        String address = "168.000.00.000";
        Member petitionMember = createMember("20241234");
        Petition petition = createPetitionEntity("제목","내용",false,petitionMember);
        when(petitionRepository.findByIdWithMember(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(redisService.isFirstConnect(address,petitionId,"petition")).thenReturn(true);
        when(petitionLikeRepository.existsByMemberAndPetition(any(),any())).thenReturn(false);

        //when
        PetitionResponseDto petitionResponseDto = petitionService.getPetition(petitionId,address,petitionMember);

        //then
        assertAll(
                ()->assertEquals(petitionResponseDto.getTitle(),"제목"),
                ()->assertEquals(petitionResponseDto.getContent(),"내용"),
                ()->assertEquals(petitionResponseDto.getHasAuthority(),true),
                ()->assertEquals(petitionResponseDto.getIsLiked(),false)
        );
        verify(petitionRepository,times(1)).findByIdWithMember(petitionId);
        verify(redisService,times(1)).isFirstConnect(any(),any(),any());
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(any(),any());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 좋아요를 누른 청원")
    public void getPetitionTestLiked(){
        //given
        Long petitionId = 1L;
        String address = "168.000.00.000";
        Member petitionMember = createMember("20241234");
        Member member = createMember("20251234");
        ReflectionTestUtils.setField(member,"id",2L);
        Petition petition = createPetitionEntity("제목","내용",false,petitionMember);
        when(petitionRepository.findByIdWithMember(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(redisService.isFirstConnect(address,petitionId,"petition")).thenReturn(true);
        when(petitionLikeRepository.existsByMemberAndPetition(any(),any())).thenReturn(true);

        //when
        PetitionResponseDto petitionResponseDto = petitionService.getPetition(petitionId,address,member);

        //then
        assertAll(
                ()->assertEquals(petitionResponseDto.getTitle(),"제목"),
                ()->assertEquals(petitionResponseDto.getContent(),"내용"),
                ()->assertEquals(petitionResponseDto.getHasAuthority(),false),
                ()->assertEquals(petitionResponseDto.getIsLiked(),true)
        );
        verify(petitionRepository,times(1)).findByIdWithMember(petitionId);
        verify(redisService,times(1)).isFirstConnect(any(),any(),any());
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(any(),any());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 실패 테스트 - 타인의 비밀청원")
    public void getPetitionFailTest(){
        //given
        Long petitionId = 1L;
        String address = "168.000.00.000";
        Member petitionMember = createMember("20241234");
        Member member = createMember("20251234");
        Petition petition = createPetitionEntity("제목","내용",true,petitionMember);
        when(petitionRepository.findByIdWithMember(petitionId)).thenReturn(Optional.ofNullable(petition));

        //when
        MyException exception = assertThrows(MyException.class,()->petitionService.getPetition(petitionId,address,null));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"비밀글입니다.");
        verify(petitionRepository,times(1)).findByIdWithMember(petitionId);
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 관리자가 비밀청원 호출")
    public void getPetitionAdminTest(){
        //given
        Long petitionId = 1L;
        String address = "168.000.00.000";
        Member petitionMember = createMember("20241234");
        Member admin = createMember("20251234");
        ReflectionTestUtils.setField(admin,"id",2L);
        ReflectionTestUtils.setField(admin,"roles",Collections.singletonList("ROLE_ADMIN"));
        Petition petition = createPetitionEntity("제목","내용",true,petitionMember);
        when(petitionRepository.findByIdWithMember(petitionId)).thenReturn(Optional.ofNullable(petition));
        when(redisService.isFirstConnect(address,petitionId,"petition")).thenReturn(true);
        when(petitionLikeRepository.existsByMemberAndPetition(any(),any())).thenReturn(false);

        //when
        PetitionResponseDto petitionResponseDto = petitionService.getPetition(petitionId,address,admin);

        //then
        assertAll(
                ()->assertEquals(petitionResponseDto.getTitle(),"제목"),
                ()->assertEquals(petitionResponseDto.getContent(),"내용"),
                ()->assertEquals(petitionResponseDto.getHasAuthority(),false),
                ()->assertEquals(petitionResponseDto.getIsLiked(),false)
        );
        verify(petitionRepository,times(1)).findByIdWithMember(petitionId);
        verify(redisService,times(1)).isFirstConnect(any(),any(),any());
        verify(petitionLikeRepository,times(1)).existsByMemberAndPetition(any(),any());
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트")
    public void getPetitionListTest(){
        //given
        Member member = createMember("20241234");
        Petition petition1 = createPetitionEntity("제목1","내용1",false,member);
        Petition petition2 = createPetitionEntity("제목2","내용2",false,member);
        Petition petition3 = createPetitionEntity("제목3","내용3",true,member);
        List<Petition> petitions = Arrays.asList(petition1,petition2,petition3);
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        Page<Petition> mockPage = new PageImpl<>(petitions,pageable,petitions.size());
        when(petitionRepository.findAllWithMember(pageable)).thenReturn(mockPage);

        //when
        ListResponseDto<PetitionListResponseDto> dto = petitionService.getPetitionList("date",1,null);

        //then
        assertAll(
                ()->assertEquals(dto.getPages(),1),
                ()->assertEquals(dto.getTotal(),petitions.size()),
                ()->assertEquals(dto.getContents().get(0).getTitle(),"제목1"),
                ()->assertEquals(dto.getContents().get(1).getTitle(),"제목2"),
                ()->assertEquals(dto.getContents().get(2).getTitle(),"비밀청원입니다.")
        );
        verify(petitionRepository,times(1)).findAllWithMember(pageable);
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트 - 본인 비밀 청원")
    public void getPetitionListUserTest(){
        //given
        Member member = createMember("20241234");
        Petition petition1 = createPetitionEntity("제목1","내용1",false,member);
        Petition petition2 = createPetitionEntity("제목2","내용2",false,member);
        Petition petition3 = createPetitionEntity("제목3","내용3",true,member);
        List<Petition> petitions = Arrays.asList(petition1,petition2,petition3);
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        Page<Petition> mockPage = new PageImpl<>(petitions,pageable,petitions.size());
        when(petitionRepository.findAllWithMember(pageable)).thenReturn(mockPage);

        //when
        ListResponseDto<PetitionListResponseDto> dto = petitionService.getPetitionList("date",1,member);

        //then
        assertAll(
                ()->assertEquals(dto.getPages(),1),
                ()->assertEquals(dto.getTotal(),petitions.size()),
                ()->assertEquals(dto.getContents().get(0).getTitle(),"제목1"),
                ()->assertEquals(dto.getContents().get(1).getTitle(),"제목2"),
                ()->assertEquals(dto.getContents().get(2).getTitle(),"제목3")
        );
        verify(petitionRepository,times(1)).findAllWithMember(pageable);
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트 - 관리자")
    public void getPetitionListAdminTest(){
        //given
        Member member = createMember("20241234");
        Member admin = createMember("20251234");
        ReflectionTestUtils.setField(admin,"roles",Collections.singletonList("ROLE_ADMIN"));
        Petition petition1 = createPetitionEntity("제목1","내용1",true,member);
        Petition petition2 = createPetitionEntity("제목2","내용2",true,member);
        Petition petition3 = createPetitionEntity("제목3","내용3",true,member);
        List<Petition> petitions = Arrays.asList(petition1,petition2,petition3);
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"));
        Page<Petition> mockPage = new PageImpl<>(petitions,pageable,petitions.size());
        when(petitionRepository.findAllWithMember(pageable)).thenReturn(mockPage);

        //when
        ListResponseDto<PetitionListResponseDto> dto = petitionService.getPetitionList("date",1,admin);

        //then
        assertAll(
                ()->assertEquals(dto.getPages(),1),
                ()->assertEquals(dto.getTotal(),petitions.size()),
                ()->assertEquals(dto.getContents().get(0).getTitle(),"제목1"),
                ()->assertEquals(dto.getContents().get(1).getTitle(),"제목2"),
                ()->assertEquals(dto.getContents().get(2).getTitle(),"제목3")
        );
        verify(petitionRepository,times(1)).findAllWithMember(pageable);
    }



    private PetitionRequestDto createPetition(String title, String content){
        return PetitionRequestDto.builder()
                .title(title)
                .content(content)
                .isPrivate(false)
                .build();
    }

    private Member createMember(String studentId){
        Member member = Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build();
        ReflectionTestUtils.setField(member,"id",1L);
        return member;
    }
    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("image","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("image","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("image","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }

    private Petition createPetitionEntity(String title, String content,boolean isPrivate,Member member){
        Petition petition = Petition.builder().title(title).content(content).isPrivate(isPrivate).member(member).build();
        ReflectionTestUtils.setField(petition,"id",1L);
        ReflectionTestUtils.setField(petition,"createDate", LocalDate.now());
        ReflectionTestUtils.setField(petition,"modifiedDate", LocalDateTime.now());
        return petition;
    }
}
