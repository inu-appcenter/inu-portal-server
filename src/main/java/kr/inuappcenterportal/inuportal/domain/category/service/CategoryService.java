package kr.inuappcenterportal.inuportal.domain.category.service;

import kr.inuappcenterportal.inuportal.domain.category.dto.CategoryDto;
import kr.inuappcenterportal.inuportal.domain.category.dto.CategoryUpdateDto;
import kr.inuappcenterportal.inuportal.domain.category.enums.CategoryType;
import kr.inuappcenterportal.inuportal.domain.category.model.Category;
import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public Long addCategory(CategoryDto categoryDto){
        if(categoryRepository.existsByCategory(categoryDto.getCategory())){
            throw new MyException(MyErrorCode.USER_DUPLICATE_CATEGORY);
        }
        return categoryRepository.save(Category.builder().category(categoryDto.getCategory()).build()).getId();
    }

    @Transactional
    public Long changeCategoryName(CategoryUpdateDto categoryUpdateDto){
        Category category = categoryRepository.findByCategory(categoryUpdateDto.getCategory()).orElseThrow(()->new MyException(MyErrorCode.CATEGORY_NOT_FOUND));
        if(categoryRepository.existsByCategory(categoryUpdateDto.getNewCategory())){
            throw new MyException(MyErrorCode.USER_DUPLICATE_CATEGORY);
        }
        category.changeName(categoryUpdateDto.getNewCategory());
        return category.getId();
    }

    @Transactional
    public void deleteCategory(String category){
        Category cate = categoryRepository.findByCategory(category).orElseThrow(()->new MyException(MyErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(cate);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories(){
        return categoryRepository.findAllByType(CategoryType.POST).stream().map(Category::getCategory).collect(Collectors.toList());
    }

    public List<String> getNoticeCategories() {
        return categoryRepository.findAllByType(CategoryType.NOTICE).stream().map(Category::getCategory).collect(Collectors.toList());
    }
}
