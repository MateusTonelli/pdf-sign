package com.pdfsign.repository;

import com.pdfsign.domain.DocumentSigner;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSignerRepository extends JpaRepository<DocumentSigner, UUID> {

    List<DocumentSigner> findByDocumentIdOrderBySortOrderAsc(UUID documentId);

    Optional<DocumentSigner> findByAccessToken(String accessToken);

    long countByDocumentIdAndStatusNot(UUID documentId, com.pdfsign.domain.SignerStatus status);
}
