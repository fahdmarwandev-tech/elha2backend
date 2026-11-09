package com.game3alhaq.repository;

import com.game3alhaq.entity.StaffEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffEmailRepository extends JpaRepository<StaffEmail, Long> {

    List<StaffEmail> findAllByOrderByCreatedAtDesc();

    boolean existsByEmailIgnoreCase(String email);

    Optional<StaffEmail> findByEmailIgnoreCase(String email);
}
