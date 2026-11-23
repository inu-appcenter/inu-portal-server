package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.member.dto.BlockResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Block", description = "차단 관리 API")
public interface ChatBlockApiSpecification {

    @Operation(summary = "유저 차단")
    @SecurityRequirement(name = "Auth")
    ResponseEntity<ResponseDto<Void>> blockUser(@PathVariable Long targetMemberId, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "게시글 작성자 차단",
            description = "PostResponseDto/PostListResponseDto는 memberId를 내려주지 않아(익명 글 재식별 방지) " +
                    "클라이언트가 대상 memberId를 알 수 없다. postId만으로 차단한다.")
    @SecurityRequirement(name = "Auth")
    ResponseEntity<ResponseDto<Void>> blockUserByPostId(@PathVariable Long postId, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "댓글/대댓글 작성자 차단",
            description = "ReplyResponseDto/ReReplyResponseDto도 memberId를 내려주지 않는다. replyId만으로 차단한다.")
    @SecurityRequirement(name = "Auth")
    ResponseEntity<ResponseDto<Void>> blockUserByReplyId(@PathVariable Long replyId, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "차단 해제")
    @SecurityRequirement(name = "Auth")
    ResponseEntity<ResponseDto<Void>> unblockUser(@PathVariable Long targetMemberId, @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "차단 목록 조회")
    @SecurityRequirement(name = "Auth")
    ResponseEntity<ResponseDto<List<BlockResponseDto>>> getBlockList(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails);
}
