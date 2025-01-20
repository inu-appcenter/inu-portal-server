package kr.inuappcenterportal.inuportal.domain.club.service;

import kr.inuappcenterportal.inuportal.domain.club.dto.ClubListResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final ClubRepository clubRepository;

    public List<ClubListResponseDto> getClubList(String category){
        if(category==null){
            return clubRepository.findAll().stream().map(ClubListResponseDto::of).collect(Collectors.toList());
        }
        else {
            return clubRepository.findByCategory(category).stream().map(ClubListResponseDto::of).collect(Collectors.toList());
        }
    }
}
