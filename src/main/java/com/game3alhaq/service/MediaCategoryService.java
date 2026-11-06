package com.game3alhaq.service;

import com.game3alhaq.dto.request.MediaRequests.CreateCategoryRequest;
import com.game3alhaq.dto.request.MediaRequests.UpdateCategoryRequest;
import com.game3alhaq.dto.response.MediaResponses.CategoryResponse;
import com.game3alhaq.entity.MediaCategory;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.MediaCategoryMapper;
import com.game3alhaq.repository.MediaCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaCategoryService {

    private final MediaCategoryRepository mediaCategoryRepository;
    private final MediaCategoryMapper mediaCategoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return mediaCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(mediaCategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        MediaCategory category = findCategoryEntity(id);
        return mediaCategoryMapper.toResponse(category);
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating media category: slug={}", request.getSlug());
        if (mediaCategoryRepository.existsBySlug(request.getSlug().trim())) {
            throw new IllegalArgumentException("Category slug already exists: " + request.getSlug());
        }

        MediaCategory category = mediaCategoryMapper.toEntity(request);
        category.setName(request.getName().trim());
        category.setSlug(request.getSlug().trim().toLowerCase());
        MediaCategory saved = mediaCategoryRepository.save(category);
        return mediaCategoryMapper.toResponse(saved);
    }

    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating media category: id={}", id);
        MediaCategory category = findCategoryEntity(id);

        applyCategoryUpdates(category, request);
        MediaCategory saved = mediaCategoryRepository.save(category);
        return mediaCategoryMapper.toResponse(saved);
    }

    public void deleteCategory(Long id) {
        log.info("Deleting media category: id={}", id);
        MediaCategory category = findCategoryEntity(id);
        mediaCategoryRepository.delete(category);
    }

    private MediaCategory findCategoryEntity(Long id) {
        return mediaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MediaCategory", "id", id));
    }

    private void applyCategoryUpdates(MediaCategory category, UpdateCategoryRequest req) {
        if (req.getName() != null && !req.getName().isBlank()) {
            category.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            category.setDescription(req.getDescription().trim());
        }
        if (req.getSortOrder() != null) {
            category.setSortOrder(req.getSortOrder());
        }
        if (req.getIsActive() != null) {
            category.setIsActive(req.getIsActive());
        }
    }
}
