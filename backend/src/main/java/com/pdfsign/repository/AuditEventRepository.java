package com.pdfsign.repository;

import com.pdfsign.domain.AuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
