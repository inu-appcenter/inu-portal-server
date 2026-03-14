package kr.inuappcenterportal.inuportal.petition;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionRequestDto;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import kr.inuappcenterportal.inuportal.domain.petition.respoitory.PetitionRepository;
import kr.inuappcenterportal.inuportal.domain.petition.service.PetitionService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PetitionTest {
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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PetitionService petitionService;
    @Autowired
    PetitionRepository petitionRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;

    @Test
    @DisplayName("총학생회 청원 등록 테스트")
    public void savePetitionTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/petitions");
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(petitionRequestDto);
        MockPart jsonPart = new MockPart("petitionRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        ResultActions resultActions =mockMvc.perform(multipartRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 등록 성공"))
                .andDo(print());

        Long petitionId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("data").asLong();

        Petition petition = petitionRepository.findByIdWithMember(petitionId).orElse(null);
        assertAll(
                ()->assertEquals(petition.getId(),petitionId),
                ()->assertEquals(petition.getTitle(),"제목"),
                ()->assertEquals(petition.getContent(),"내용"),
                ()->assertEquals(petition.getMember(),member),
                ()->assertEquals(petition.getImageCount(),images.size()),
                ()->assertEquals(petition.getIsPrivate(),false)
        );
    }

    @Test
    @DisplayName("총학생회 청원 등록 테스트 - 이미지 없이 등록")
    public void savePetitionNoImageTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/petitions");
        String body = objectMapper.writeValueAsString(petitionRequestDto);
        MockPart jsonPart = new MockPart("petitionRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        ResultActions resultActions =mockMvc.perform(multipartRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 등록 성공"))
                .andDo(print());

        Long petitionId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("data").asLong();

        Petition petition = petitionRepository.findByIdWithMember(petitionId).orElse(null);
        assertAll(
                ()->assertEquals(petition.getId(),petitionId),
                ()->assertEquals(petition.getTitle(),"제목"),
                ()->assertEquals(petition.getContent(),"내용"),
                ()->assertEquals(petition.getMember(),member),
                ()->assertEquals(petition.getImageCount(),0),
                ()->assertEquals(petition.getIsPrivate(),false)
        );
    }

    @Test
    @DisplayName("총학생회 청원 수정 테스트")
    public void updatePetitionTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        List<MultipartFile> images = createDummyImages();
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,images);
        PetitionRequestDto petitionUpdateDto = createPetitionRequest("수정된 제목","수정된 내용",true);
        List<MultipartFile> updatedImages = createDummyImages2();

        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/petitions/" +petitionsId)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        for(MultipartFile multipartFile : updatedImages){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(petitionUpdateDto);
        MockPart jsonPart = new MockPart("petitionRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총삭생회 청원 수정 성공"))
                .andExpect(jsonPath("$.data").value(petitionsId))
                .andDo(print());

        Petition petition = petitionRepository.findByIdWithMember(petitionsId).orElse(null);
        assertAll(
                ()->assertEquals(petition.getTitle(),"수정된 제목"),
                ()->assertEquals(petition.getContent(),"수정된 내용"),
                ()->assertEquals(petition.getId(),petitionsId),
                ()->assertEquals(petition.getImageCount(),updatedImages.size()),
                ()->assertEquals(petition.getIsPrivate(),true)
        );
    }

    @Test
    @DisplayName("총학생회 청원 삭제 테스트")
    public void deletePetitionTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,null);
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(delete("/api/petitions/"+petitionsId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 삭제 성공"))
                .andExpect(jsonPath("$.data").value(petitionsId))
                .andDo(print());

        assertNull(petitionRepository.findByIdWithMember(petitionsId).orElse(null));
    }

    @Test
    @DisplayName("총학생회 청원 삭제 실패 테스트 - 다른 사람의 청원")
    public void deletePetitionFailTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member petitionMember = saveMember("20241234");
        Member member = saveMember("20251111");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,petitionMember,null);
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(delete("/api/petitions/"+petitionsId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("이 게시글의 수정/삭제에 대한 권한이 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 비로그인 조회")
    public void getPetitionTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,null);
        mockMvc.perform(get("/api/petitions/"+petitionsId).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"))
                .andExpect(jsonPath("$.data.writer").value(20241234))
                .andExpect(jsonPath("$.data.imageCount").value(0))
                .andExpect(jsonPath("$.data.hasAuthority").value(false))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 본인의 비밀글")
    public void getPetitionSecretTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",false);
        Member member = saveMember("20241234");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,null);
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(get("/api/petitions/"+petitionsId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"))
                .andExpect(jsonPath("$.data.writer").value(20241234))
                .andExpect(jsonPath("$.data.imageCount").value(0))
                .andExpect(jsonPath("$.data.hasAuthority").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 실패 테스트 - 비밀청원")
    public void getPetitionFailTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",true);
        Member member = saveMember("20241234");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,null);
        mockMvc.perform(get("/api/petitions/"+petitionsId).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("비밀글입니다."));
    }

    @Test
    @DisplayName("총학생회 청원 가져오기 테스트 - 관리자가 비밀글 호출")
    public void getPetitionAdminTest() throws Exception {
        PetitionRequestDto petitionRequestDto = createPetitionRequest("제목","내용",true);
        Member member = saveMember("20241234");
        Long petitionsId = petitionService.savePetition(petitionRequestDto,member,null);
        Member admin = saveAdminMember("20255555");
        TokenDto tokenDto = memberService.login(admin);
        mockMvc.perform(get("/api/petitions/"+petitionsId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"))
                .andExpect(jsonPath("$.data.writer").value(20241234))
                .andExpect(jsonPath("$.data.imageCount").value(0))
                .andExpect(jsonPath("$.data.hasAuthority").value(false))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트")
    public void getPetitionListTest() throws Exception {
        Member member = saveMember("20241234");
        PetitionRequestDto petitionRequestDto1 = createPetitionRequest("제목1","내용1",false);
        PetitionRequestDto petitionRequestDto2 = createPetitionRequest("제목2","내용2",false);
        PetitionRequestDto petitionRequestDto3 = createPetitionRequest("제목3","내용3",true);
        petitionService.savePetition(petitionRequestDto1,member,null);
        petitionService.savePetition(petitionRequestDto2,member,null);
        petitionService.savePetition(petitionRequestDto3,member,null);
        mockMvc.perform(get("/api/petitions").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 리스트 가져오기 성공"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.contents[2].title").value("제목1"))
                .andExpect(jsonPath("$.data.contents[1].title").value("제목2"))
                .andExpect(jsonPath("$.data.contents[0].title").value("비밀청원입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트 - 본인 비밀청원 포함")
    public void getPetitionListUserTest() throws Exception {
        Member member = saveMember("20241234");
        PetitionRequestDto petitionRequestDto1 = createPetitionRequest("제목1","내용1",false);
        PetitionRequestDto petitionRequestDto2 = createPetitionRequest("제목2","내용2",false);
        PetitionRequestDto petitionRequestDto3 = createPetitionRequest("제목3","내용3",true);
        petitionService.savePetition(petitionRequestDto1,member,null);
        petitionService.savePetition(petitionRequestDto2,member,null);
        petitionService.savePetition(petitionRequestDto3,member,null);
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(get("/api/petitions").header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 리스트 가져오기 성공"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.contents[2].title").value("제목1"))
                .andExpect(jsonPath("$.data.contents[1].title").value("제목2"))
                .andExpect(jsonPath("$.data.contents[0].title").value("제목3"))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 청원 리스트 가져오기 테스트 - 관리자")
    public void getPetitionListAdminTest() throws Exception {
        Member member = saveMember("20241234");
        PetitionRequestDto petitionRequestDto1 = createPetitionRequest("제목1","내용1",true);
        PetitionRequestDto petitionRequestDto2 = createPetitionRequest("제목2","내용2",true);
        PetitionRequestDto petitionRequestDto3 = createPetitionRequest("제목3","내용3",true);
        petitionService.savePetition(petitionRequestDto1,member,null);
        petitionService.savePetition(petitionRequestDto2,member,null);
        petitionService.savePetition(petitionRequestDto3,member,null);
        Member admin = saveAdminMember("20255555");
        TokenDto tokenDto = memberService.login(admin);
        mockMvc.perform(get("/api/petitions").header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 청원 리스트 가져오기 성공"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.contents[2].title").value("제목1"))
                .andExpect(jsonPath("$.data.contents[1].title").value("제목2"))
                .andExpect(jsonPath("$.data.contents[0].title").value("제목3"))
                .andDo(print());

    }

    private PetitionRequestDto createPetitionRequest(String title, String content, boolean isPrivate){
        return PetitionRequestDto.builder().title(title).content(content).isPrivate(isPrivate).build();
    }

    private Member saveMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }
    private Member saveAdminMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_ADMIN")).build());
    }
    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("images","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("images","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("images","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }

    private List<MultipartFile> createDummyImages2(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("images","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("images","image2.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2);
    }
}
