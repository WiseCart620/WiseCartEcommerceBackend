package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.GuestEmailVerification;
import com.wisecartecommerce.ecommerce.entity.GuestEmailVerification.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuestEmailVerificationRepository extends JpaRepository<GuestEmailVerification, Long> {

    @Query("SELECT v FROM GuestEmailVerification v WHERE v.emailNormalized = :emailNormalized "
            + "AND v.purpose = :purpose AND v.verified = false ORDER BY v.createdAt DESC")
    List<GuestEmailVerification> findRecentUnverifiedByNormalizedEmail(
            @Param("emailNormalized") String emailNormalized, @Param("purpose") OtpPurpose purpose);

    boolean existsByEmailNormalizedAndPurposeAndVerifiedTrueAndVerifiedAtAfter(
            String emailNormalized, OtpPurpose purpose, LocalDateTime after);

    Optional<GuestEmailVerification> findFirstByEmailNormalizedAndPurposeOrderByCreatedAtDesc(
            String emailNormalized, OtpPurpose purpose);
}