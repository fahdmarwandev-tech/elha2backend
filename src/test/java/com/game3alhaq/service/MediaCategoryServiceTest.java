package com.game3alhaq.service;

import com.game3alhaq.dto.request.MediaRequests.CreateCategoryRequest;
import com.game3alhaq.dto.request.MediaRequests.UpdateCategoryRequest;
import com.game3alhaq.dto.response.MediaResponses.CategoryResponse;
import com.game3alhaq.entity.MediaCategory;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.MediaCategoryMapper;
import com.game3alhaq.repository.MediaCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaCategoryServiceTest {

    @Mock
    private MediaCategoryRepository mediaCategoryRepository;

    @Mock
    private MediaCategoryMapper mediaCategoryMapper;

    @InjectMocks
    private MediaCategoryService mediaCategoryService;

    private MediaCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = MediaCategory.builder()
                .id(1L)
                .name("دروس ومواعظ")
                .slug("lessons")
                .description("محاضرات ودروس دينية")
                .sortOrder(1)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return all active categories ordered by sortOrder")
    void testGetAllCategories() {
        when(mediaCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(sampleCategory));
        when(mediaCategoryMapper.toResponse(sampleCategory)).thenReturn(
                CategoryResponse.builder().id(1L).name("دروس ومواعظ").slug("lessons").build());

        List<CategoryResponse> list = mediaCategoryService.getAllCategories();

        assertEquals(1, list.size());
        assertEquals("lessons", list.get(0).getSlug());
        verify(mediaCategoryRepository, times(1)).findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Test
    @DisplayName("Should return category by ID")
    void testGetCategoryById() {
        when(mediaCategoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(mediaCategoryMapper.toResponse(sampleCategory)).thenReturn(
                CategoryResponse.builder().id(1L).name("دروس ومواعظ").slug("lessons").build());

        CategoryResponse res = mediaCategoryService.getCategoryById(1L);

        assertNotNull(res);
        assertEquals(1L, res.getId());
    }

    @Test
    @DisplayName("Should create category successfully when slug is unique")
    void testCreateCategorySuccess() {
        CreateCategoryRequest req = CreateCategoryRequest.builder()
                .name("خطب الجمعة")
                .slug("khutbah")
                .description("خطب الجمعة الأسبوعية")
                .sortOrder(2)
                .isActive(true)
                .build();

        when(mediaCategoryRepository.existsBySlug("khutbah")).thenReturn(false);
        when(mediaCategoryMapper.toEntity(req)).thenReturn(new MediaCategory());
        when(mediaCategoryRepository.save(any(MediaCategory.class))).thenReturn(sampleCategory);
        when(mediaCategoryMapper.toResponse(any(MediaCategory.class))).thenReturn(
                CategoryResponse.builder().id(2L).name("خطب الجمعة").slug("khutbah").build());

        CategoryResponse res = mediaCategoryService.createCategory(req);

        assertNotNull(res);
        verify(mediaCategoryRepository, times(1)).save(any(MediaCategory.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when slug already exists")
    void testCreateCategoryDuplicateSlug() {
        CreateCategoryRequest req = CreateCategoryRequest.builder()
                .name("دروس")
                .slug("lessons")
                .build();

        when(mediaCategoryRepository.existsBySlug("lessons")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> mediaCategoryService.createCategory(req));
        verify(mediaCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdateCategory() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder()
                .name("دروس ومحاضرات محدثة")
                .sortOrder(5)
                .build();

        when(mediaCategoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(mediaCategoryRepository.save(sampleCategory)).thenReturn(sampleCategory);
        when(mediaCategoryMapper.toResponse(sampleCategory)).thenReturn(
                CategoryResponse.builder().id(1L).name("دروس ومحاضرات محدثة").sortOrder(5).build());

        CategoryResponse res = mediaCategoryService.updateCategory(1L, req);

        assertNotNull(res);
        assertEquals("دروس ومحاضرات محدثة", res.getName());
        assertEquals(5, res.getSortOrder());
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDeleteCategory() {
        when(mediaCategoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

        mediaCategoryService.deleteCategory(1L);

        verify(mediaCategoryRepository, times(1)).delete(sampleCategory);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent category")
    void testUpdateCategoryNotFound() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder().name("New").build();
        when(mediaCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mediaCategoryService.updateCategory(99L, req));
    }
}
