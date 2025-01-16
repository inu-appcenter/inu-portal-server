package kr.inuappcenterportal.inuportal.domain.petition.service;

import kr.inuappcenterportal.inuportal.domain.petitionLike.model.PetitionLike;
import kr.inuappcenterportal.inuportal.domain.petitionLike.repository.PetitionLikeRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionListResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionRequestDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import kr.inuappcenterportal.inuportal.domain.petition.respoitory.PetitionRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetitionService {

    private final PetitionRepository petitionRepository;
    private final ImageService imageService;
    private final RedisService redisService;
    private final PetitionLikeRepository petitionLikeRepository;

    @Value("${petitionImagePath}")
    private String path;

    @Transactional
    public Long savePetition(PetitionRequestDto petitionRequestDto, Member member, List<MultipartFile> images) throws IOException {
        Petition petition = Petition.builder()
                .title(petitionRequestDto.getTitle())
                .content(petitionRequestDto.getContent())
                .isPrivate(petitionRequestDto.getIsPrivate())
                .member(member)
                .build();
        petition = petitionRepository.save(petition);
        if (images != null) {
            petition.updateImageCount(images.size());
            imageService.saveImage(petition.getId(), images, path);
        }
        return petition.getId();
    }

    public byte[] getPetitionImage(Long petitionId, Long imageId){
        return imageService.getImage(petitionId,imageId,path);
    }

    @Transactional
    public Long updatePetition(Long petitionId,PetitionRequestDto petitionRequestDto, Member member, List<MultipartFile> images) throws IOException {
        Petition petition = validHasAuthorizationPetition(petitionId,member);
        petition.updatePetition(petitionRequestDto.getTitle(),petitionRequestDto.getContent(),petitionRequestDto.getIsPrivate());
        if(images!=null){
            imageService.updateImage(petitionId,petition.getImageCount(),images,path);
            petition.updateImageCount(images.size());
        }
        return petitionId;
    }


    @Transactional
    public void deletePetition(Long petitionId, Member member){
        Petition petition = validHasAuthorizationPetition(petitionId,member);
        petition.deletePetition();
    }

    @Transactional
    public int likePetition(Long petitionId, Member member){
        Petition petition = petitionRepository.findByIdWithLock(petitionId).orElseThrow(()->new MyException(MyErrorCode.NOT_FOUND_PETITION));
        if(petition.getMember().equals(member)){
            throw new MyException(MyErrorCode.NOT_LIKE_MY_POST);
        }
        if(petitionLikeRepository.existsByMemberAndPetition(member,petition)){
            PetitionLike petitionLike = petitionLikeRepository.findPetitionLikeByMemberAndPetition(member,petition).orElseThrow(()->new MyException(MyErrorCode.USER_OR_POST_NOT_FOUND));
            petitionLikeRepository.delete(petitionLike);
            petition.downLike();
            return -1;
        }
        else {
            PetitionLike petitionLike =PetitionLike.builder()
                    .member(member)
                    .petition(petition)
                    .build();
            petitionLikeRepository.save(petitionLike);
            petition.upLike();
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public PetitionResponseDto getPetition(Long petitionId, String address, Member member){
        Petition petition = petitionRepository.findByIdWithMember(petitionId).orElseThrow(()->new MyException(MyErrorCode.NOT_FOUND_PETITION));
        if (petition.getIsPrivate() && (member==null||(!petition.getMember().equals(member) && !member.getRoles().contains("ROLE_ADMIN")))) {
            throw new MyException(MyErrorCode.SECRET_PETITION);
        }
        if(redisService.isFirstConnect(address,petitionId,"petition")){
            redisService.insertAddress(address,petitionId,"petition");
            petition.upViewCount();
        }
        boolean hasAuthority = false;
        boolean isLiked = false;
        if(petition.getMember().equals(member)){
            hasAuthority = true;
        }
        if(petitionLikeRepository.existsByMemberAndPetition(member,petition)){
            isLiked = true;
        }
        return PetitionResponseDto.of(petition,hasAuthority,isLiked,petition.getMember().getStudentId());
    }

    @Transactional(readOnly = true)
    public ListResponseDto<PetitionListResponseDto> getPetitionList(String sort, int page,Member member){
        Pageable pageable = PageRequest.of(page>0?--page:page,8,sortData(sort));
        Page<Petition> dto = petitionRepository.findAllWithMember(pageable);
        List<PetitionListResponseDto> petitions = dto.stream().map(petition -> {
            if(member!=null&&member.getRoles().contains("ROLE_ADMIN")){
                return PetitionListResponseDto.of(petition, petition.getMember().getStudentId());
            }
            if(petition.getIsPrivate()&&!petition.getMember().equals(member)){
                return PetitionListResponseDto.secretPetition(petition,petition.getMember().getStudentId());
            }
            else {
                return PetitionListResponseDto.of(petition, petition.getMember().getStudentId());
            }
            }).toList();
        return ListResponseDto.of(dto.getTotalPages(), dto.getTotalElements(), petitions);
    }


    private Petition validHasAuthorizationPetition(Long petitionId, Member member){
        Petition petition = petitionRepository.findByIdAndIsDeletedFalse(petitionId).orElseThrow(()->new MyException(MyErrorCode.NOT_FOUND_PETITION));
        if(!petition.getMember().getId().equals(member.getId())){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        return petition;
    }


    private Sort sortData(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "id");
        }
        else if(sort.equals("like")){
            return Sort.by(Sort.Direction.DESC, "good","id");
        }
        else if(sort.equals("view")){
            return Sort.by(Sort.Direction.DESC, "view","id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }


}
