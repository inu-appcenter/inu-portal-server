package kr.inuappcenterportal.inuportal.club;

import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingRequestDto;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import kr.inuappcenterportal.inuportal.domain.club.repository.ClubRepository;
import kr.inuappcenterportal.inuportal.domain.club.service.ClubService;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ClubServiceTest {
    @InjectMocks
    private ClubService clubService;

    @Mock
    private ClubRepository clubRepository;
    @Mock
    ImageService imageService;


    @Test
    @DisplayName("동아리 모집공고를 등록합니다.")
    public void addRecruitTest() throws IOException {
        //given
        Club club = makeClub();
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        List<MultipartFile> images = createDummyImages();
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집글",true);

        //when
        Long clubId = clubService.addRecruit(club.getId(),requestDto,images);

        //then
        Assertions.assertEquals(clubId,club.getId());
    }

    @Test
    @DisplayName("동아리 모집공고를 수정합니다.")
    public void updateRecruitTest() throws IOException {
        //given
        Club club = makeClub();
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        List<MultipartFile> images = createDummyImages();
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집글",true);

        //when
        Long clubId = clubService.updateRecruit(club.getId(),requestDto,images);

        //then
        Assertions.assertEquals(clubId,club.getId());
    }

    @Test
    @DisplayName("동아리의 모집공고를 가져옵니다.")
    public void getRecruitTest(){
        //given
        Club club = makeClub();
        club.recruiting("모집 공고",3L,true);
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));

        //when
        ClubRecruitingResponseDto responseDto = clubService.getRecruit(club.getId());

        //then
        Assertions.assertAll(
                ()->Assertions.assertEquals(responseDto.getImageCount(),3),
                ()->Assertions.assertEquals(responseDto.getRecruit(),"모집 공고")
        );

    }

    private Club makeClub(){
        Club club = Club.builder().name("testClub").build();
        ReflectionTestUtils.setField(club,"id",1L);
        return club;
    }

    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("image","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("image","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("image","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }
}
