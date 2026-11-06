package com.game3alhaq.repository;

import com.game3alhaq.entity.MediaCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaCategoryRepository extends JpaRepository<MediaCategory, Long> {

    List<MediaCategory> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<MediaCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
