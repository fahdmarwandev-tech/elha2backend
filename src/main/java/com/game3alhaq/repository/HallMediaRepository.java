package com.game3alhaq.repository;

import com.game3alhaq.entity.HallMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallMediaRepository extends JpaRepository<HallMedia, Long> {

    List<HallMedia> findAllByHallIdOrderBySortOrderAsc(Long hallId);
}
