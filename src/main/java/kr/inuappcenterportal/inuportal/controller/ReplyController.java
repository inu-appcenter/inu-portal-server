package kr.inuappcenterportal.inuportal.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.dto.ReplyDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Replies", description = "댓글 API")
@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReplyController {
    private final TokenProvider tokenProvider;
    private final ReplyService replyService;

    @Operation(summary = "댓글 등록",description = "헤더 Auth에 발급받은 토큰을,url 파라미터에 게시글의 id, 바디에 {content,bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "댓글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{postId}")
    public ResponseEntity<?> saveReply(HttpServletRequest httpServletRequest, @RequestBody ReplyDto replyDto, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("댓글 등록 호출");
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        return new ResponseEntity<>(new ResponseDto<>(replyService.saveReply(memberId, replyDto,postId),"댓글 등록 성공"), HttpStatus.CREATED);
    }

    @Operation(summary = "댓글 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id, 바디에 {content, bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "댓글 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 댓글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{replyId}")
    public ResponseEntity<?> updateReply(HttpServletRequest httpServletRequest, @RequestBody ReplyDto replyDto, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 수정 호출 id:{}",replyId);
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));;
        return new ResponseEntity<>(new ResponseDto<>(replyService.updateReply(memberId, replyDto,replyId),"댓글 수정 성공"), HttpStatus.OK);

    }

    @Operation(summary = "댓글 삭제",description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 댓글의 id를 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "댓글 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 댓글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{replyId}")
    public ResponseEntity<?> deleteReply(HttpServletRequest httpServletRequest, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 삭제 호출 id:{}",replyId);
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        replyService.delete(memberId, replyId);
        return new ResponseEntity<>(new ResponseDto<>(replyId,"댓글 삭제 성공"), HttpStatus.OK);

    }

    @Operation(summary = "대댓글 등록",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id, 바디에 {content, bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 댓글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "게시글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/reReplies/{replyId}")
    public ResponseEntity<?> saveReReply(HttpServletRequest httpServletRequest, @RequestBody ReplyDto replyDto, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        log.info("댓글 저장 호출");
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        return new ResponseEntity<>(new ResponseDto<>(replyService.saveReReply(memberId, replyDto,replyId),"대댓글 저장 성공"), HttpStatus.CREATED);
    }

    @Operation(summary = "댓글 좋아요 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 댓글의 id를 보내주세요. 좋아요 시 {data:1}, 좋아요 취소 시 {data:-1}입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 좋아요 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 댓글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/like/{replyId}")
    public ResponseEntity<?> likePost(HttpServletRequest httpServletRequest, @Parameter(name = "replyId",description = "댓글의 id",in = ParameterIn.PATH)@PathVariable Long replyId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("댓글 좋아요 여부 변경 호출 id:{}",replyId);
        return new ResponseEntity<>(new ResponseDto<>(replyService.likeReply(memberId,replyId),"게시글 좋아요 여부 변경성공"), HttpStatus.OK);
    }

}
