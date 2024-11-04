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
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.ReplyDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@Slf4j
@Tag(name = "Replies", description = "댓글 API")
@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReplyController {
    private final ReplyService replyService;

    @Operation(summary = "댓글 등록",description = "헤더 Auth에 발급받은 토큰을,url 파라미터에 게시글의 id, 바디에 {content,bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "댓글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "일정 시간 동안 같은 게시글이나 댓글을 작성할 수 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{postId}")
    public ResponseEntity<ResponseDto<Long>> saveReply(@AuthenticationPrincipal Member member, @Valid @RequestBody ReplyDto replyDto, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId) throws NoSuchAlgorithmException {
        log.info("댓글 등록 호출");
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(replyService.saveReply(member, replyDto,postId),"댓글 등록 성공"));
    }

    @Operation(summary = "댓글 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id, 바디에 {content, bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "댓글 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 댓글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{replyId}")
    public ResponseEntity<ResponseDto<Long>> updateReply(@AuthenticationPrincipal Member member, @Valid@RequestBody ReplyDto replyDto, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 수정 호출 id:{}",replyId);
        return ResponseEntity.ok(ResponseDto.of(replyService.updateReply(member.getId(), replyDto,replyId),"댓글 수정 성공"));

    }

    @Operation(summary = "댓글 삭제",description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 댓글의 id를 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "댓글 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 댓글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{replyId}")
    public ResponseEntity<ResponseDto<Long>> deleteReply(@AuthenticationPrincipal Member member, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 삭제 호출 id:{}",replyId);
        replyService.delete(member.getId(), replyId);
        return ResponseEntity.ok(ResponseDto.of(replyId,"댓글 삭제 성공"));

    }

    @Operation(summary = "대댓글 등록",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id, 바디에 {content, bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "게시글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "일정 시간 동안 같은 게시글이나 댓글을 작성할 수 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다. / 존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{replyId}/re-replies")
    public ResponseEntity<ResponseDto<Long>> saveReReply(@AuthenticationPrincipal Member member, @Valid @RequestBody ReplyDto replyDto, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId) throws NoSuchAlgorithmException {
        log.info("댓글 저장 호출");
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(replyService.saveReReply(member, replyDto,replyId),"대댓글 저장 성공"));
    }

    @Operation(summary = "댓글 좋아요 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id를 보내주세요. 좋아요 시 {data:1}, 좋아요 취소 시 {data:-1}입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 좋아요 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "자신의 댓글에는 추천을 할 수 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{replyId}/like")
    public ResponseEntity<ResponseDto<Integer>> likePost(@AuthenticationPrincipal Member member, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 좋아요 여부 변경 호출 id:{}",replyId);
        return ResponseEntity.ok(ResponseDto.of(replyService.likeReply(member,replyId),"게시글 좋아요 여부 변경성공"));
    }

}
