package com.pdfsign.service;

import com.pdfsign.domain.Document;
import com.pdfsign.domain.DocumentSigner;
import com.pdfsign.domain.SignatureField;
import com.pdfsign.web.dto.DocumentDetailDto;
import com.pdfsign.web.dto.DocumentSummaryDto;
import com.pdfsign.web.dto.SignerDto;
import com.pdfsign.web.dto.SignatureFieldDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DocumentMapper {

    private DocumentMapper() {
    }

    public static DocumentSummaryDto toSummary(Document document) {
        return new DocumentSummaryDto(
                document.getId(),
                document.getTitle(),
                document.getSignatureType(),
                document.getStatus(),
                document.getContentHashSha256(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    public static DocumentDetailDto toDetail(Document document, List<DocumentSigner> signers, List<SignatureField> fields) {
        Map<UUID, String> signerEmails =
                signers.stream().collect(Collectors.toMap(DocumentSigner::getId, DocumentSigner::getEmail));
        List<SignerDto> signerDtos = signers.stream()
                .map(s -> new SignerDto(
                        s.getId(),
                        s.getName(),
                        s.getEmail(),
                        s.getStatus(),
                        s.getSignedAt(),
                        s.getSortOrder()))
                .toList();
        List<SignatureFieldDto> fieldDtos = fields.stream()
                .map(f -> new SignatureFieldDto(
                        f.getId(),
                        signerEmails.get(f.getSignerId()),
                        f.getPage(),
                        f.getX(),
                        f.getY(),
                        f.getWidth(),
                        f.getHeight()))
                .toList();
        return new DocumentDetailDto(
                document.getId(),
                document.getTitle(),
                document.getSignatureType(),
                document.getStatus(),
                document.getContentHashSha256(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                signerDtos,
                fieldDtos);
    }
}
