package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeListDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeRequestDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeResponseDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import kr.inuappcenterportal.inuportal.domain.councilNotice.repostiory.CouncilRepository;
import kr.inuappcenterportal.inuportal.domain.councilNotice.service.CouncilNoticeService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouncilServiceTest {
    @InjectMocks
    CouncilNoticeService councilNoticeService;

    @Mock
    CouncilRepository councilRepository;

    @Mock
    ImageService imageService;

    @Mock
    RedisService redisService;

    @Test
    @DisplayName("총학생회 공지 등록 테스트")
    public void saveCouncilNoticeTest(){
        //given
        CouncilNoticeRequestDto requestDto = createCouncilNoticeRequest("테스트 공지 제목","테스트 내용");

        //when
        councilNoticeService.saveCouncilNotice(requestDto);

        //then
        verify(councilRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("총학생회 공지 이미지 저장 테스트")
    public void saveCouncilNoticeImageTest() throws NoSuchFieldException, IllegalAccessException, IOException {
        //given
        CouncilNotice councilNotice = createCouncilNotice("테스트 제목","테스트 내용");
        List<MultipartFile> images = createDummyImages();
        when(councilRepository.findById(councilNotice.getId())).thenReturn(Optional.of(councilNotice));

        //when
        councilNoticeService.saveCouncilNoticeImage(councilNotice.getId(),images);

        //then
        verify(imageService, times(1)).saveImage(any(),any(),any());
    }

    @Test
    @DisplayName("총학생회 공지 수정 테스트")
    public void updateCouncilNoticeTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        CouncilNotice councilNotice = createCouncilNotice("테스트 제목","테스트 내용");
        CouncilNoticeRequestDto requestDto = createCouncilNoticeRequest("테스트 제목 수정","테스트 내용 수정");
        when(councilRepository.findById(councilNotice.getId())).thenReturn(Optional.of(councilNotice));

        //when
        Long id = councilNoticeService.updateCouncilNotice(councilNotice.getId(),requestDto);

        //then
        assertEquals(id,councilNotice.getId());
    }

    @Test
    @DisplayName("총학생회 공지 이미지 수정 테스트")
    public void updateCouncilNoticeImageTest() throws NoSuchFieldException, IllegalAccessException, IOException {
        //given
        CouncilNotice councilNotice = createCouncilNotice("테스트 제목","테스트 내용");
        List<MultipartFile> images = createDummyImages();
        when(councilRepository.findById(councilNotice.getId())).thenReturn(Optional.of(councilNotice));

        //when
        Long id = councilNoticeService.updateCouncilNoticeImage(councilNotice.getId(),images);

        //then
        assertEquals(id,councilNotice.getId());
        verify(imageService, times(1)).updateImage(any(),any(long.class),any(),any());
    }

    @Test
    @DisplayName("총학생회 공지 삭제 테스트")
    public void deleteCouncilNoticeTest() throws NoSuchFieldException, IllegalAccessException, IOException {
        //given
        CouncilNotice councilNotice = createCouncilNotice("테스트 제목","테스트 내용");
        when(councilRepository.findById(councilNotice.getId())).thenReturn(Optional.of(councilNotice));

        //when
        councilNoticeService.deleteCouncilNotice(councilNotice.getId());

        //then
        verify(councilRepository, times(1)).delete(any());
        verify(imageService, times(1)).deleteAllImage(any(),any(),any());
    }

    @Test
    @DisplayName("총학생회 공지 가져오기 테스트")
    public void getCouncilNoticeTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        CouncilNotice councilNotice = createCouncilNotice("테스트 제목","테스트 내용");
        when(councilRepository.findById(councilNotice.getId())).thenReturn(Optional.of(councilNotice));
        when(redisService.isFirstConnect(any(),any(),any())).thenReturn(true);

        //when
        CouncilNoticeResponseDto responseDto = councilNoticeService.getCouncilNotice(councilNotice.getId(),"address");

        //then
        assertAll(
                ()->assertEquals(responseDto.getView(),1),
                ()->assertEquals(responseDto.getImageCount(),0),
                ()->assertEquals(responseDto.getId(),1L),
                ()->assertEquals(responseDto.getTitle(),"테스트 제목"),
                ()->assertEquals(responseDto.getContent(),"테스트 내용")
        );
    }

    @Test
    @DisplayName("총학생회 공지사항 이미지 가져오기 테스트")
    public void getCouncilNoticeImageTest(){
        //given
        Long councilNoticeId = 1L;
        Long imageId = 1L;

        //when
        councilNoticeService.getCouncilNoticeImage(councilNoticeId,imageId);

        //then
        verify(imageService, times(1)).getImage(any(),any(),any());
    }

    @Test
    @DisplayName("총학생회 공지 리스트 가져오기 테스트")
    public void getCouncilNoticeListTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        CouncilNotice councilNotice1 = createCouncilNotice("제목1","내용1");
        CouncilNotice councilNotice2 = createCouncilNotice("제목2","내용2");
        CouncilNotice councilNotice3 = createCouncilNotice("제목3","내용3");
        List<CouncilNotice> councilNotices = Arrays.asList(councilNotice1,councilNotice2,councilNotice3);
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createDate","id"));
        Page<CouncilNotice> mockPage = new PageImpl<>(councilNotices,pageable,councilNotices.size());
        when(councilRepository.findAllBy(pageable)).thenReturn(mockPage);

        //when
        ListResponseDto<CouncilNoticeListDto> dto = councilNoticeService.getCouncilNoticeList("date",1);

        //then
        assertAll(
                ()->assertEquals(dto.getPages(),1),
                ()->assertEquals(dto.getTotal(),3),
                ()->assertEquals(dto.getContents().get(0).getTitle(),"제목1"),
                ()->assertEquals(dto.getContents().get(1).getTitle(),"제목2"),
                ()->assertEquals(dto.getContents().get(2).getTitle(),"제목3")
        );
    }

    private CouncilNoticeRequestDto createCouncilNoticeRequest(String title, String content){
        return CouncilNoticeRequestDto.builder()
                .title(title)
                .content(content)
                .build();
    }

    private CouncilNotice createCouncilNotice(String title, String content) throws NoSuchFieldException, IllegalAccessException {
        CouncilNotice councilNotice = CouncilNotice.builder()
                .title(title)
                .content(content)
                .build();
        Class<?> c = councilNotice.getClass();
        Field filed = c.getDeclaredField("id");
        filed.setAccessible(true);
        filed.set(councilNotice,1L);

        Class<?> c2 = councilNotice.getClass().getSuperclass();
        Field f2 = c2.getDeclaredField("createDate");
        f2.setAccessible(true);
        f2.set(councilNotice,LocalDate.now());

        Field f3 = c2.getDeclaredField("modifiedDate");
        f3.setAccessible(true);
        f3.set(councilNotice,LocalDate.now());
        return councilNotice;
    }

    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("image","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("image","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("image","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }
}
