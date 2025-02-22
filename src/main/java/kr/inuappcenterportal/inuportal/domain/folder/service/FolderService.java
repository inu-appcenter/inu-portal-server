package kr.inuappcenterportal.inuportal.domain.folder.service;

import kr.inuappcenterportal.inuportal.domain.folder.dto.FolderDto;
import kr.inuappcenterportal.inuportal.domain.folder.dto.FolderPostDto;
import kr.inuappcenterportal.inuportal.domain.folder.dto.FolderResponseDto;
import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import kr.inuappcenterportal.inuportal.domain.folder.repository.FolderRepository;
import kr.inuappcenterportal.inuportal.domain.folderPost.model.FolderPost;
import kr.inuappcenterportal.inuportal.domain.folderPost.repository.FolderPostRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Long createFolder(Member member, FolderDto folderDto ){
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
        if(folderPostDto.getPostId().isEmpty()){
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
        if(folderPostDto.getPostId().isEmpty()){
            throw new MyException(MyErrorCode.POST_SCRAP_LIST_NOT_FOUND);
        }
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        for(Long id:folderPostDto.getPostId()){
            Post post = postRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
            folderPostRepository.delete(folderPostRepository.findByFolderAndPost(folder,post).orElseThrow(()->new MyException(MyErrorCode.FOLDER_OR_POST_NOT_FOUND)));
        }
    }

    @Transactional(readOnly = true)
    public List<FolderResponseDto> getFolder(Member member){
        return folderRepository.findAllByMember(member).stream().map(FolderResponseDto::of).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListResponseDto<PostListResponseDto> getPostInFolder(Long folderId, String sort, int page) {
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        List<PostListResponseDto> folderDto = folderPostRepository.findAllByFolder(folder,postService.sortFetchJoin(sort)).stream().map(file -> postService.getPostListResponseDto(file.getPost())).collect(Collectors.toList());
        return postService.pagingFetchJoin(page,folderDto);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<PostListResponseDto> searchPostInFolder(Long folderId, String query, String sort, int page) {
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new MyException(MyErrorCode.FOLDER_NOT_FOUND));
        List<PostListResponseDto> folderDto = folderPostRepository.searchInFolder(folder,query,postService.sortFetchJoin(sort)).stream().map(file -> postService.getPostListResponseDto(file.getPost())).collect(Collectors.toList());
        return postService.pagingFetchJoin(page,folderDto);
    }



}
