package com.game3alhaq.repository;

import com.game3alhaq.entity.PrayersAndHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrayersAndHistoryRepository extends JpaRepository<PrayersAndHistory, Long> {

    Optional<PrayersAndHistory> findTopByOrderByEffectiveDateDesc();

    Optional<PrayersAndHistory> findFirstByOrderByIdAsc();
}
