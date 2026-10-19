package com.pdfsign.repository;

import com.pdfsign.domain.UserCertificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCertificateRepository extends JpaRepository<UserCertificate, UUID> {

    List<UserCertificate> findByUserIdAndActiveTrueOrderByValidToDesc(UUID userId);

    Optional<UserCertificate> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserCertificate> findFirstByUserIdAndActiveTrue(UUID userId);

    @Modifying
    @Query("update UserCertificate c set c.active = false where c.userId = :userId and c.active = true")
    void deactivateAllForUser(@Param("userId") UUID userId);

    boolean existsByUserIdAndActiveTrue(UUID userId);
}
