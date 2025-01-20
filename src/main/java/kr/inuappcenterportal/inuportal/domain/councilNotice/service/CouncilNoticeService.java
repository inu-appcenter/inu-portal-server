package kr.inuappcenterportal.inuportal.domain.councilNotice.service;

import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeListDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeRequestDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeResponseDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import kr.inuappcenterportal.inuportal.domain.councilNotice.repostiory.CouncilRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CouncilNoticeService {
    private final CouncilRepository councilRepository;
    private final ImageService imageService;
    private final RedisService redisService;
    @Value("${councilNoticeImagePath}")
    private String path;

    public Long saveCouncilNotice(CouncilNoticeRequestDto councilNoticeRequestDto){
        CouncilNotice councilNotice = CouncilNotice.builder().title(councilNoticeRequestDto.getTitle()).content(councilNoticeRequestDto.getContent()).build();
        councilRepository.save(councilNotice);
        return councilNotice.getId();
    }

    public Long saveCouncilNoticeImage(Long councilNoticeId, List<MultipartFile> images) throws IOException {
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_COUNCIL_NOTICE));
        if (images != null) {
            councilNotice.updateImageCount(images.size());
            imageService.saveImageWithThumbnail(councilNotice.getId(),images,path);
        }
        return councilNoticeId;
    }

    public Long updateCouncilNotice(Long councilNoticeId, CouncilNoticeRequestDto councilNoticeRequestDto){
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_COUNCIL_NOTICE));
        councilNotice.updateCouncilNotice(councilNoticeRequestDto.getTitle(), councilNoticeRequestDto.getContent());
        return councilNotice.getId();
    }

    public Long updateCouncilNoticeImage(Long councilNoticeId, List<MultipartFile> images) throws IOException {
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_COUNCIL_NOTICE));
        if(images!=null){
            imageService.updateImages(councilNoticeId,images,path);
            councilNotice.updateImageCount(images.size());
        }
        return councilNotice.getId();
    }

    public void deleteCouncilNotice(Long councilNoticeId) throws IOException {
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_COUNCIL_NOTICE));
        imageService.deleteAllImage(councilNoticeId,councilNotice.getImageCount(),path);
        councilRepository.delete(councilNotice);
    }

    @Transactional
    public CouncilNoticeResponseDto getCouncilNotice(Long councilNoticeId, String address){
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElseThrow(()-> new MyException(MyErrorCode.NOT_FOUND_COUNCIL_NOTICE));
        if(redisService.isFirstConnect(address,councilNoticeId,"councilNotice")){
            redisService.insertAddress(address,councilNoticeId,"councilNotice");
            councilNotice.upViewCount();
        }
        return CouncilNoticeResponseDto.of(councilNotice);
    }
    @Transactional(readOnly = true)
    public byte[] getCouncilNoticeImage(Long councilNoticeId, Long imageId){
        return imageService.getImage(councilNoticeId,imageId,path);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<CouncilNoticeListDto> getCouncilNoticeList(String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8,sort(sort));
        Page<CouncilNotice> notices = councilRepository.findAllBy(pageable);
        return ListResponseDto.of(notices.getTotalPages(),notices.getTotalElements(),notices.getContent().stream().map(CouncilNoticeListDto::of).collect(Collectors.toList()));
    }


    private Sort sort(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "id");
        }
        else if(sort.equals("view")) {
            return Sort.by(Sort.Direction.DESC, "view", "id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

}
