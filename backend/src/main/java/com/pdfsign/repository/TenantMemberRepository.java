package com.pdfsign.repository;

import com.pdfsign.domain.TenantMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMemberRepository extends JpaRepository<TenantMember, UUID> {

    Optional<TenantMember> findFirstByUserId(UUID userId);
}
