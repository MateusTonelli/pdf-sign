package com.pdfsign.service;

import com.pdfsign.config.AppProperties;
import com.pdfsign.domain.Document;
import com.pdfsign.domain.DocumentSigner;
import com.pdfsign.domain.DocumentStatus;
import com.pdfsign.domain.SignatureField;
import com.pdfsign.domain.SignatureType;
import com.pdfsign.domain.SignerStatus;
import com.pdfsign.repository.DocumentRepository;
import com.pdfsign.repository.DocumentSignerRepository;
import com.pdfsign.repository.SignatureFieldRepository;
import com.pdfsign.repository.UserCertificateRepository;
import com.pdfsign.security.AuthUser;
import com.pdfsign.storage.DocumentStorage;
import com.pdfsign.web.dto.DocumentDetailDto;
import com.pdfsign.web.dto.DocumentSummaryDto;
import com.pdfsign.web.dto.SignerInputDto;
import com.pdfsign.web.dto.SignatureFieldInputDto;
import com.pdfsign.web.dto.UpdateDocumentRequest;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentSignerRepository documentSignerRepository;
    private final SignatureFieldRepository signatureFieldRepository;
    private final DocumentStorage documentStorage;
    private final UserCertificateRepository userCertificateRepository;
    private final TokenGenerator tokenGenerator;
    private final AppProperties appProperties;
    private final EmailService emailService;
    private final AuditService auditService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentSignerRepository documentSignerRepository,
            SignatureFieldRepository signatureFieldRepository,
            DocumentStorage documentStorage,
            UserCertificateRepository userCertificateRepository,
            TokenGenerator tokenGenerator,
            AppProperties appProperties,
            EmailService emailService,
            AuditService auditService) {
        this.documentRepository = documentRepository;
        this.documentSignerRepository = documentSignerRepository;
        this.signatureFieldRepository = signatureFieldRepository;
        this.documentStorage = documentStorage;
        this.userCertificateRepository = userCertificateRepository;
        this.tokenGenerator = tokenGenerator;
        this.appProperties = appProperties;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public List<DocumentSummaryDto> list(AuthUser authUser) {
        return documentRepository.findByTenantIdOrderByCreatedAtDesc(authUser.getTenantId()).stream()
                .map(document -> {
                    List<DocumentSigner> signers =
                            documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId());
                    return DocumentMapper.toSummary(document, signers, appProperties.getPublicBaseUrl());
                })
                .toList();
    }

    @Transactional
    public DocumentDetailDto create(AuthUser authUser, MultipartFile file, String title) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("PDF file is required");
        }
        UUID id = UUID.randomUUID();
        DocumentStorage.StoredFile stored = documentStorage.store(file, authUser.getTenantId(), id);
        Instant now = Instant.now();
        String docTitle = (title != null && !title.isBlank()) ? title.trim() : defaultTitle(file.getOriginalFilename());
        Document document = new Document(
                id,
                authUser.getTenantId(),
                authUser.getUserId(),
                docTitle,
                stored.storagePath(),
                stored.sha256(),
                SignatureType.SIMPLE,
                DocumentStatus.DRAFT,
                now,
                now);
        documentRepository.save(document);
        auditService.record(document.getId(), document.getTenantId(), "DOCUMENT_CREATED", authUser.getEmail(), docTitle, null);
        return toDetail(document);
    }

    public DocumentDetailDto get(AuthUser authUser, UUID id) {
        Document document = requireTenantDocument(authUser, id);
        return toDetail(document);
    }

    @Transactional
    public DocumentDetailDto update(AuthUser authUser, UUID id, UpdateDocumentRequest request) {
        Document document = requireDraft(authUser, id);
        if (request.title() != null && !request.title().isBlank()) {
            document.setTitle(request.title().trim());
        }
        if (request.signatureType() != null) {
            document.setSignatureType(request.signatureType());
        }
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        return toDetail(document);
    }

    @Transactional
    public DocumentDetailDto replaceSigners(AuthUser authUser, UUID id, List<SignerInputDto> signers) {
        Document document = requireDraft(authUser, id);
        if (signers == null || signers.isEmpty()) {
            throw new BadRequestException("At least one signer is required");
        }
        signatureFieldRepository.deleteByDocumentId(document.getId());
        documentSignerRepository.deleteAll(documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId()));

        Instant expires = Instant.now().plus(appProperties.getMagicTokenTtlDays(), ChronoUnit.DAYS);
        int order = 0;
        for (SignerInputDto signer : signers) {
            DocumentSigner entity = new DocumentSigner(
                    UUID.randomUUID(),
                    document.getId(),
                    signer.name().trim(),
                    signer.email().trim().toLowerCase(),
                    tokenGenerator.generateUrlSafeToken(32),
                    expires,
                    SignerStatus.PENDING,
                    order++);
            documentSignerRepository.save(entity);
        }
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        return toDetail(document);
    }

    @Transactional
    public DocumentDetailDto replaceFields(AuthUser authUser, UUID id, List<SignatureFieldInputDto> fields) {
        Document document = requireDraft(authUser, id);
        List<DocumentSigner> signers = documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId());
        Map<String, DocumentSigner> byEmail = new HashMap<>();
        for (DocumentSigner signer : signers) {
            byEmail.put(signer.getEmail().toLowerCase(), signer);
        }
        signatureFieldRepository.deleteByDocumentId(document.getId());
        if (fields != null) {
            for (SignatureFieldInputDto field : fields) {
                DocumentSigner signer = byEmail.get(field.signerEmail().trim().toLowerCase());
                if (signer == null) {
                    throw new BadRequestException("Unknown signer email: " + field.signerEmail());
                }
                validateNormalized(field.x(), field.y(), field.width(), field.height());
                SignatureField entity = new SignatureField(
                        UUID.randomUUID(),
                        document.getId(),
                        signer.getId(),
                        field.page(),
                        field.x(),
                        field.y(),
                        field.width(),
                        field.height());
                signatureFieldRepository.save(entity);
            }
        }
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        return toDetail(document);
    }

    @Transactional
    public DocumentDetailDto send(AuthUser authUser, UUID id, String ip) {
        Document document = requireDraft(authUser, id);
        if (document.getSignatureType() == SignatureType.CERTIFICATE
                && !userCertificateRepository.existsByUserIdAndActiveTrue(authUser.getUserId())) {
            throw new BadRequestException("Certificate signing requires an active certificate on your account");
        }
        List<DocumentSigner> signers = documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId());
        if (signers.isEmpty()) {
            throw new BadRequestException("Add signers before sending");
        }
        Instant expires = Instant.now().plus(appProperties.getMagicTokenTtlDays(), ChronoUnit.DAYS);
        String base = appProperties.getPublicBaseUrl().replaceAll("/$", "");
        for (DocumentSigner signer : signers) {
            signer.setAccessToken(tokenGenerator.generateUrlSafeToken(32));
            signer.setTokenExpiresAt(expires);
            signer.setStatus(SignerStatus.PENDING);
            documentSignerRepository.save(signer);
            String link = base + "/sign/" + signer.getAccessToken();
            emailService.sendSigningInvitation(signer.getEmail(), signer.getName(), document.getTitle(), link);
        }
        document.setStatus(DocumentStatus.PENDING);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        auditService.record(document.getId(), document.getTenantId(), "DOCUMENT_SENT", authUser.getEmail(), null, ip);
        return toDetail(document);
    }

    @Transactional
    public DocumentDetailDto cancel(AuthUser authUser, UUID id, String ip) {
        Document document = requireTenantDocument(authUser, id);
        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new BadRequestException("Completed documents cannot be cancelled");
        }
        document.setStatus(DocumentStatus.CANCELLED);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        auditService.record(document.getId(), document.getTenantId(), "DOCUMENT_CANCELLED", authUser.getEmail(), null, ip);
        return toDetail(document);
    }

    public byte[] download(AuthUser authUser, UUID id) throws IOException {
        Document document = requireTenantDocument(authUser, id);
        return documentStorage.readBytes(document.getStoragePath());
    }

    private Document requireTenantDocument(AuthUser authUser, UUID id) {
        return documentRepository
                .findByIdAndTenantId(id, authUser.getTenantId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private Document requireDraft(AuthUser authUser, UUID id) {
        Document document = requireTenantDocument(authUser, id);
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new BadRequestException("Document is not in DRAFT status");
        }
        return document;
    }

    private DocumentDetailDto toDetail(Document document) {
        List<DocumentSigner> signers = documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId());
        List<SignatureField> fields = signatureFieldRepository.findByDocumentId(document.getId());
        return DocumentMapper.toDetail(document, signers, fields, appProperties.getPublicBaseUrl());
    }

    private static void validateNormalized(double x, double y, double w, double h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > 1.001 || y + h > 1.001) {
            throw new BadRequestException("Field coordinates must be normalized between 0 and 1");
        }
    }

    private static String defaultTitle(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Untitled document";
        }
        return filename;
    }
}
