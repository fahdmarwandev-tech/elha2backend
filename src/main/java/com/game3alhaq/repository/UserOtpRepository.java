package com.game3alhaq.repository;

import com.game3alhaq.entity.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {

    Optional<UserOtp> findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            String identifier, String purpose);
}
