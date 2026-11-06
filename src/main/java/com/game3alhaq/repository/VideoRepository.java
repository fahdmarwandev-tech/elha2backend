package com.game3alhaq.repository;

import com.game3alhaq.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findAllByOrderByCreatedAtDesc();

    List<Video> findByCategoryOrderByCreatedAtDesc(String category);

    Optional<Video> findFirstByIsFeaturedTrueOrderByCreatedAtDesc();

    boolean existsByYoutubeId(String youtubeId);

    @Modifying
    @Query("UPDATE Video v SET v.isFeatured = false WHERE v.isFeatured = true")
    void clearFeaturedVideos();

    @Modifying
    @Query("UPDATE Video v SET v.isFeatured = true WHERE v.id = :id")
    void setFeaturedVideo(@Param("id") Long id);
}
