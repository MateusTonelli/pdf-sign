package com.pdfsign.repository;

import com.pdfsign.domain.SignatureField;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface SignatureFieldRepository extends JpaRepository<SignatureField, UUID> {

    List<SignatureField> findByDocumentId(UUID documentId);

    List<SignatureField> findBySignerId(UUID signerId);

    @Modifying
    void deleteByDocumentId(UUID documentId);
}
