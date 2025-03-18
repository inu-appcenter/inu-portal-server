package kr.inuappcenterportal.inuportal.domain.club.service;

import kr.inuappcenterportal.inuportal.domain.club.dto.ClubListResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingRequestDto;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import kr.inuappcenterportal.inuportal.domain.club.repository.ClubRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final ClubRepository clubRepository;
    private final ImageService imageService;

    @Value("${clubRecruitImagePath}")
    private String path;
    public List<ClubListResponseDto> getClubList(String category){
        if(category==null){
            return clubRepository.findAll().stream().map(ClubListResponseDto::from).collect(Collectors.toList());
        }
        else {
            return clubRepository.findByCategory(category).stream().map(ClubListResponseDto::from).collect(Collectors.toList());
        }
    }

    @Transactional
    public Long addRecruit(Long clubId, ClubRecruitingRequestDto requestDto, List<MultipartFile> images) throws IOException {
        Club club = clubRepository.findById(clubId).orElseThrow( ()-> new MyException(MyErrorCode.NOT_FOUND_CLUB));
        if(images!=null){
            imageService.saveImage(clubId,images,path);
        }
        else{
            images = new ArrayList<>();
        }
        club.recruiting(requestDto.getRecruit(), (long) images.size(),requestDto.getIs_recruiting());
        return clubId;
    }

    @Transactional
    public Long updateRecruit(Long clubId, ClubRecruitingRequestDto requestDto, List<MultipartFile> images) throws IOException {
        Club club = clubRepository.findById(clubId).orElseThrow( ()-> new MyException(MyErrorCode.NOT_FOUND_CLUB));
        if(images!=null){
            imageService.updateImages(clubId,images,path);
        }
        else{
            images = new ArrayList<>();
        }
        club.recruiting(requestDto.getRecruit(), (long) images.size(),requestDto.getIs_recruiting());
        return clubId;
    }

    @Transactional(readOnly = true)
    public ClubRecruitingResponseDto getRecruit(Long clubId){
        Club club = clubRepository.findById(clubId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_CLUB));
        return ClubRecruitingResponseDto.from(club);
    }
}
