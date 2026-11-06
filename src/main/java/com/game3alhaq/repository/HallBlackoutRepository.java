package com.game3alhaq.repository;

import com.game3alhaq.entity.HallBlackout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HallBlackoutRepository extends JpaRepository<HallBlackout, Long> {

    List<HallBlackout> findAllByHallIdAndBlackoutDateBetween(Long hallId, LocalDate start, LocalDate end);

    List<HallBlackout> findAllByHallIdAndBlackoutDate(Long hallId, LocalDate blackoutDate);

    List<HallBlackout> findAllByHallIdOrderByBlackoutDateAsc(Long hallId);

    List<HallBlackout> findAllByOrderByBlackoutDateAsc();
}
