package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.Folder;
import kr.inuappcenterportal.inuportal.domain.FolderPost;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.dto.FolderDto;
import kr.inuappcenterportal.inuportal.dto.FolderPostDto;
import kr.inuappcenterportal.inuportal.dto.FolderResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyDuplicateException;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.repository.FolderPostRepository;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.repository.FolderRepository;
import kr.inuappcenterportal.inuportal.repository.PostRepository;
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


    @Transactional
    public Long createFolder(Long memberId, FolderDto folderDto ){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return folderRepository.save(Folder.builder().name(folderDto.getName()).member(member).build()).getId();
    }

    @Transactional
    public Long updateFolder(Long folderId, FolderDto folderDto){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_NOT_FOUND));
        folder.update(folderDto.getName());
        return folderId;
    }

    @Transactional
    public void deleteFolder(Long folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_NOT_FOUND));
        folderRepository.delete(folder);
    }

    @Transactional
    public Long insertPost(FolderPostDto folderPostDto){
        Folder folder = folderRepository.findById(folderPostDto.getFolderId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_NOT_FOUND));
        Post post = postRepository.findById(folderPostDto.getPostId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(folderPostRepository.existsByFolderAndPost(folder,post)){
            System.out.println(folder.getName());
            System.out.println(post.getId());
            throw new MyDuplicateException(MyErrorCode.POST_DUPLICATE_FOLDER);
        }
        return folderPostRepository.save(FolderPost.builder().post(post).folder(folder).build()).getId();
    }

    @Transactional
    public void deleteInFolder(FolderPostDto folderPostDto){
        Folder folder = folderRepository.findById(folderPostDto.getFolderId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_NOT_FOUND));
        Post post = postRepository.findById(folderPostDto.getFolderId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        folderPostRepository.delete(folderPostRepository.findByFolderAndPost(folder,post).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_OR_POST_NOT_FOUND)));
    }

    @Transactional
    public List<FolderResponseDto> getFolder(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return folderRepository.findAllByMember(member).stream().map(FolderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public List<PostListResponseDto> getPostInFolder(Long folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new MyNotFoundException(MyErrorCode.FOLDER_NOT_FOUND));
        return folderPostRepository.findAllByFolder(folder).stream().map(file->postService.getPostListResponseDto(file.getPost())).collect(Collectors.toList());
    }



}
