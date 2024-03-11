package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.FolderDto;
import kr.inuappcenterportal.inuportal.dto.FolderPostDto;
import kr.inuappcenterportal.inuportal.dto.FolderResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final FolderPostRepository folderPostRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final ScrapRepository scrapRepository;


    @Transactional
    public Long createFolder(Long memberId, FolderDto folderDto ){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        return folderRepository.save(Folder.builder().name(folderDto.getName()).member(member).build()).getId();
    }

    @Transactional
    public Long updateFolder(Long folderId, FolderDto folderDto){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        folder.update(folderDto.getName());
        return folderId;
    }

    @Transactional
    public void deleteFolder(Long folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        folderRepository.delete(folder);
    }

    @Transactional
    public Long insertInFolder(Long folderId, FolderPostDto folderPostDto){
        if(folderPostDto.getPostId().size()==0){
            throw new MyException(MyErrorCode.POST_SCRAP_LIST_NOT_FOUND);
        }
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        Member member =memberRepository.findById(folder.getMember().getId()).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        for(Long id:folderPostDto.getPostId()){
            Post post = postRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
            Scrap scrap = scrapRepository.findByMemberAndPost(member,post).orElseThrow(()->new MyException(MyErrorCode.SCRAP_NOT_FOUND));
            if(folderPostRepository.existsByFolderAndPost(folder,post)){
                throw new MyException(MyErrorCode.POST_DUPLICATE_FOLDER);
            }
            folderPostRepository.save(FolderPost.builder().post(post).folder(folder).scrap(scrap).build());
        }
        return folder.getId();
    }

    @Transactional
    public void deleteInFolder(Long folderId, FolderPostDto folderPostDto){
        if(folderPostDto.getPostId().size()==0){
            throw new MyException(MyErrorCode.POST_SCRAP_LIST_NOT_FOUND);
        }
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        for(Long id:folderPostDto.getPostId()){
            Post post = postRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
            folderPostRepository.delete(folderPostRepository.findByFolderAndPost(folder,post).orElseThrow(()->new MyException(MyErrorCode.FOLDER_OR_POST_NOT_FOUND)));
        }
    }

    @Transactional(readOnly = true)
    public List<FolderResponseDto> getFolder(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        return folderRepository.findAllByMember(member).stream().map(FolderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostInFolder(Long folderId, String sort,int page) {
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        List<PostListResponseDto> folderDto = folderPostRepository.findAllByFolder(folder).stream().map(file -> postService.getPostListResponseDto(file.getPost())).sorted(Comparator.comparing(PostListResponseDto::getId).reversed()).collect(Collectors.toList());
        page--;
        int startIndex = 0;
        int endIndex = 0;
        if(page*5>folderDto.size()){
            folderDto.clear();
        }
        else{
            startIndex = page*5;
            endIndex  = Math.min((page + 1) * 5, folderDto.size());
        }
        return postService.postListSort(folderDto,sort).subList(startIndex,endIndex);
    }



}
