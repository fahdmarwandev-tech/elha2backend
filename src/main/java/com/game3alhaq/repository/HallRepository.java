package com.game3alhaq.repository;

import com.game3alhaq.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {

    Optional<Hall> findBySlug(String slug);

    List<Hall> findAllByIsActiveTrue();

    @Query("SELECT DISTINCT h FROM Hall h LEFT JOIN FETCH h.mediaList WHERE h.isActive = true")
    List<Hall> findAllActiveWithMedia();

    @Query("SELECT h FROM Hall h LEFT JOIN FETCH h.mediaList WHERE h.id = :id")
    Optional<Hall> findByIdWithMedia(@Param("id") Long id);
}
