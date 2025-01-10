package kr.inuappcenterportal.inuportal.domain.book.controller;

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
import kr.inuappcenterportal.inuportal.domain.book.dto.BookDetail;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookRegister;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.domain.book.service.BookService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.*;


@Tag(name = "Books", description = "책 벼룩시장 API")
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "책 등록", description = "헤더 Auth에 발급받은 토큰을, 바디에 {name,author,content, int 형태의 price} 보내주세요. 성공 시 등록된 책의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "책 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto<Long>> register(@RequestPart @Valid BookRegister request, @RequestPart List<MultipartFile> images,
                                                      @AuthenticationPrincipal Member member) throws IOException {
        return ResponseEntity.status(CREATED).body(ResponseDto.of(bookService.register(Book.create(request.getName(), request.getAuthor(), request.getPrice(), request.getContent(), member), images), "책 등록 성공"));
    }

    @Operation(summary = "책 리스트 조회", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 책의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "책 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ListResponseDto<BookPreview>> getList(@RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.status(OK).body(bookService.getList(page));
    }

    @Operation(summary = "책 상세 조회", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 책의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "책 상세 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<ResponseDto<BookDetail>> get(@Parameter(name = "bookId",description = "책의 id",in = ParameterIn.PATH) @PathVariable Long bookId) {
        return ResponseEntity.status(OK).body(ResponseDto.of(bookService.get(bookId), "책 상세 조회 성공"));
    }

    @Operation(summary = "책 판매 상태 변경", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 책의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "책 판매 상태 변경 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{bookId}")
    public ResponseEntity<ResponseDto<Long>> toggleTransactionStatus(@Parameter(name = "bookId",description = "책의 id",in = ParameterIn.PATH) @PathVariable Long bookId) {
        return ResponseEntity.status(OK).body(ResponseDto.of(bookService.toggleTransactionStatus(bookId), "책 판매 상태 변경 완료"));
    }

    @Operation(summary = "판매 가능한 책 리스트 조회", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 책의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "판매 가능한 책 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping("/available")
    public ResponseEntity<ListResponseDto<BookPreview>> getListOnlyAvailable(@RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.status(OK).body(bookService.getListOnlyAvailable(page));
    }
}
