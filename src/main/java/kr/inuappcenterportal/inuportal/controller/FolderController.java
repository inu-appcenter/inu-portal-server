package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Folders", description = "스크랩폴더 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;

    @Operation(summary = "스크랩폴더 생성",description = "헤더 Auth에 발급받은 토큰을,바디에 {name}을 json 형식으로 보내주세요. 성공 시 생성된 스크랩폴더의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "폴더 생성 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> createFolder(@Valid @RequestBody FolderDto folderDto, @AuthenticationPrincipal Member member){
        log.info("스크랩폴더 생성 호출 id:{}",member.getId());
        return ResponseEntity.status( HttpStatus.CREATED).body(ResponseDto.of(folderService.createFolder(member,folderDto),"폴더 생성 성공"));
    }

    @Operation(summary = "스크랩폴더명 수정",description = " url 파라미터에 스크랩폴더의 id, 바디에 {name}을 json 형식으로 보내주세요. 성공 시 수정된 스크랩폴더의 데이터베이스 폴더 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 폴더입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{folderId}")
    public ResponseEntity<ResponseDto<Long>> updateFolder(@Parameter(name = "folderId",description = "폴더의 id",in = ParameterIn.PATH) @PathVariable Long folderId, @Valid@RequestBody FolderDto folderDto){
        log.info("스크랩폴더명 수정 호출 id:{}",folderId);
        return ResponseEntity.ok(ResponseDto.of(folderService.updateFolder(folderId,folderDto),"스크랩폴더명 수정 성공"));
    }

    @Operation(summary = "스크랩폴더 삭제",description = "url 파라미터에 스크랩폴더의 id를 보내주세요. 성공 시 삭제된 스크랩폴더의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 폴더입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{folderId}")
    public ResponseEntity<ResponseDto<Long>> deleteFolder(@Parameter(name = "folderId",description = "스크랩폴더의 id",in = ParameterIn.PATH) @PathVariable Long folderId){
        log.info("스크랩폴더 삭제 호출 id:{}",folderId);
        folderService.deleteFolder(folderId);
        return ResponseEntity.ok(ResponseDto.of(folderId,"스크랩폴더 삭제 성공"));
    }

    @Operation(summary = "스크랩폴더에 게시글 담기",description = "url 파라미터에 스크랩폴더의 id를, 바디에 {postId(게시글의 데이터베이스 id값)}을 json 형식으로 보내주세요. 성공 시 스크랩폴더의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "스크랩폴더에 게시글 담기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 스크랩폴더입니다. / 존재하지 않는 게시글입니다. / 스크랩하지 않은 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "스크랩폴더에 존재하는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{folderId}/posts")
    public ResponseEntity<ResponseDto<Long>> insertPost(@PathVariable Long folderId, @Valid @RequestBody FolderPostDto folderDto){
        log.info("스크랩폴더에 게시글 담기 호출 스크랩폴더 id:{}, 게시글 id:{}",folderId,folderDto.getPostId());
        return ResponseEntity.ok(ResponseDto.of(folderService.insertInFolder(folderId, folderDto),"스크랩폴더에 게시글 담기 성공"));
    }

    @Operation(summary = "스크랩폴더에서 게시글 빼기",description = "url 파라미터에 스크랩폴더의 id를, 바디에 {postId(게시글의 데이터베이스 id값)}을 json 형식으로 보내주세요. 성공 시 스크랩폴더의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "스크랩폴더에서 게시글 빼기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 폴더입니다. / 존재하지 않는 게시글입니다. / 스크랩폴더나 게시글이 존재하지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{folderId}/posts")
    public ResponseEntity<ResponseDto<Long>> deleteInFolder(@PathVariable Long folderId, @Valid @RequestBody FolderPostDto folderPostDto){
        log.info("스크랩폴더에서 게시글 빼기 호출 스크랩폴더 id:{} 게시글 id:{}",folderId,folderPostDto.getPostId());
        folderService.deleteInFolder(folderId, folderPostDto);
        return ResponseEntity.ok(ResponseDto.of(folderId,"스크랩폴더에서 게시글 빼기 성공"));
    }

    @Operation(summary = "회원의 모든 스크랩 폴더 가져오기",description = "헤더 Auth에 발급받은 토큰을 보내주세요. 회원의 모든 스크랩 폴더가 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원의 모든 스크랩폴더 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<FolderResponseDto>>> getFolder(@AuthenticationPrincipal Member member){
        log.info("회원의 모든 스크랩폴더 가져오기 호출 폴더 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(folderService.getFolder(member),"회원의 모든 스크랩폴더 가져오기 성공"));
    }

    @Operation(summary = "스크랩폴더의 모든 게시글 가져오기",description = "url 파라미터에 스크랩폴더의 id, 정렬기준 sort(date/공백(최신순), like,scrap),페이지(공백일 시 1)를 보내주세요. 스크랩폴더의 모든 게시글이 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "스크랩폴더의 모든 게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 스크랩폴더입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{folderId}")
    public ResponseEntity<ResponseDto<ListResponseDto>> getPostInFolder(@PathVariable Long folderId, @RequestParam(required = false) String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        log.info("스크랩폴더의 모든 게시글 가져오기 호출 폴더 id:{}",folderId);
        return ResponseEntity.ok(ResponseDto.of(folderService.getPostInFolder(folderId, sort,page),"스크랩폴더의 모든 게시글 가져오기 성공"));
    }


}
