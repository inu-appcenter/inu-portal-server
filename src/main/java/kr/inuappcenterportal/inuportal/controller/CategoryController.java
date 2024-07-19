package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.dto.CategoryDto;
import kr.inuappcenterportal.inuportal.dto.CategoryUpdateDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Categories", description = "카테고리 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "카테고리 추가",description = "바디에 {category}을 json 형식으로 보내주세요. 성공 시 등록한 카테고리의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "카테고리 추가 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "동일한 카테고리가 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> addCategory(@Valid @RequestBody CategoryDto categoryDto){
        log.info("카테고리 추가 호출 카테고리명 :{}", categoryDto.getCategory());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(categoryService.addCategory(categoryDto),"카테고리 추가 성공"));
    }

    @Operation(summary = "카테고리명 변경",description = "바디에 {category, newCategory}을 json 형식으로 보내주세요. 성공 시 등록한 카테고리의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "카테고리명 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "동일한 카테고리가 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 카테고리입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("")
    public ResponseEntity<ResponseDto<Long>> updateCategory(@Valid@RequestBody CategoryUpdateDto categoryUpdateDto){
        log.info("카테고리 변경 호출 카테고리명:{}",categoryUpdateDto.getCategory());
        return ResponseEntity.ok(ResponseDto.of(categoryService.changeCategoryName(categoryUpdateDto),"카테고리명 변경 성공"));
    }

    @Operation(summary = "카테고리 삭제",description = "바디에 {category}을 json 형식으로 보내주세요. 성공 시 등록한 카테고리의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "카테고리 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "동일한 카테고리가 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 카테고리입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("")
    public ResponseEntity<ResponseDto<String>> deleteCategory(@Valid@RequestBody CategoryDto categoryDto){
        log.info("카테고리 삭제 호출 카테고리명:{}", categoryDto.getCategory());
        categoryService.deleteCategory(categoryDto.getCategory());
        return ResponseEntity.ok(ResponseDto.of(categoryDto.getCategory(),"카테고리 삭제 성공"));
    }

    @Operation(summary = "모든 카테고리 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 카테고리 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<String>>> getCategoryList(){
        return ResponseEntity.ok(ResponseDto.of(categoryService.getCategories(),"모든 카테고리 가져오기 성공"));
    }
}
