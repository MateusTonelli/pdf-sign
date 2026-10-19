package com.pdfsign.service;

import com.pdfsign.domain.Document;
import com.pdfsign.domain.DocumentSigner;
import com.pdfsign.domain.DocumentStatus;
import com.pdfsign.domain.SignatureField;
import com.pdfsign.domain.SignatureType;
import com.pdfsign.domain.SignerStatus;
import com.pdfsign.domain.User;
import com.pdfsign.domain.UserCertificate;
import com.pdfsign.pdf.PdfCertificateSigner;
import com.pdfsign.pdf.PdfSimpleSigner;
import com.pdfsign.repository.DocumentRepository;
import com.pdfsign.repository.DocumentSignerRepository;
import com.pdfsign.repository.SignatureFieldRepository;
import com.pdfsign.repository.UserCertificateRepository;
import com.pdfsign.repository.UserRepository;
import com.pdfsign.storage.DocumentStorage;
import com.pdfsign.web.dto.CertificateSignRequest;
import com.pdfsign.web.dto.SignSessionDto;
import com.pdfsign.web.dto.SignatureFieldDto;
import com.pdfsign.web.dto.SimpleSignRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignService {

    private final DocumentSignerRepository documentSignerRepository;
    private final DocumentRepository documentRepository;
    private final SignatureFieldRepository signatureFieldRepository;
    private final UserRepository userRepository;
    private final UserCertificateRepository userCertificateRepository;
    private final DocumentStorage documentStorage;
    private final CryptoService cryptoService;
    private final PdfSimpleSigner pdfSimpleSigner;
    private final PdfCertificateSigner pdfCertificateSigner;
    private final AuditService auditService;

    public SignService(
            DocumentSignerRepository documentSignerRepository,
            DocumentRepository documentRepository,
            SignatureFieldRepository signatureFieldRepository,
            UserRepository userRepository,
            UserCertificateRepository userCertificateRepository,
            DocumentStorage documentStorage,
            CryptoService cryptoService,
            PdfSimpleSigner pdfSimpleSigner,
            PdfCertificateSigner pdfCertificateSigner,
            AuditService auditService) {
        this.documentSignerRepository = documentSignerRepository;
        this.documentRepository = documentRepository;
        this.signatureFieldRepository = signatureFieldRepository;
        this.userRepository = userRepository;
        this.userCertificateRepository = userCertificateRepository;
        this.documentStorage = documentStorage;
        this.cryptoService = cryptoService;
        this.pdfSimpleSigner = pdfSimpleSigner;
        this.pdfCertificateSigner = pdfCertificateSigner;
        this.auditService = auditService;
    }

    public SignSessionDto getSession(String token) {
        DocumentSigner signer = requireValidSigner(token);
        Document document = documentRepository
                .findById(signer.getDocumentId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
        List<SignatureField> fields = signatureFieldRepository.findBySignerId(signer.getId());
        List<SignatureFieldDto> fieldDtos = fields.stream()
                .map(f -> new SignatureFieldDto(null, signer.getEmail(), f.getPage(), f.getX(), f.getY(), f.getWidth(), f.getHeight()))
                .toList();

        var userOpt = userRepository.findByEmailIgnoreCase(signer.getEmail());
        boolean canCert = false;
        String hint = null;
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            var certOpt = userCertificateRepository.findFirstByUserIdAndActiveTrue(user.getId());
            if (certOpt.isPresent()) {
                canCert = true;
                hint = "Certificate available: " + certOpt.get().getCn();
            } else {
                hint = "Account found but no active certificate uploaded";
            }
        } else {
            hint = "Upload a certificate in your account using the same email to sign with a digital certificate";
        }

        return new SignSessionDto(
                document.getTitle(),
                document.getSignatureType(),
                signer.getName(),
                signer.getEmail(),
                signer.getStatus(),
                fieldDtos,
                canCert,
                hint);
    }

    @Transactional
    public void signSimple(String token, SimpleSignRequest request, String ip, String userAgent) throws IOException {
        DocumentSigner signer = requirePendingSigner(token);
        Document document = loadSignableDocument(signer);
        if (document.getSignatureType() != SignatureType.SIMPLE) {
            throw new BadRequestException("This document requires certificate signing");
        }
        if (request.typedName() == null || request.typedName().isBlank()) {
            if (request.signatureImageBase64() == null || request.signatureImageBase64().isBlank()) {
                throw new BadRequestException("Provide typedName or signatureImageBase64");
            }
        }
        List<SignatureField> fields = signatureFieldRepository.findBySignerId(signer.getId());
        byte[] pdf = documentStorage.readBytes(document.getStoragePath());
        byte[] signed = pdfSimpleSigner.applySimpleSignature(
                pdf, fields, request.typedName(), request.signatureImageBase64());
        documentStorage.writeBytes(document.getStoragePath(), signed);
        markSigned(signer, document, ip, userAgent, "SIMPLE_SIGN");
    }

    @Transactional
    public void signCertificate(String token, CertificateSignRequest request, String ip, String userAgent) throws IOException {
        DocumentSigner signer = requirePendingSigner(token);
        Document document = loadSignableDocument(signer);
        if (document.getSignatureType() != SignatureType.CERTIFICATE) {
            throw new BadRequestException("This document uses simple signing only");
        }
        User user = userRepository
                .findByEmailIgnoreCase(signer.getEmail())
                .orElseThrow(() -> new BadRequestException(
                        "No registered user with this email. Register and upload a certificate first."));
        UserCertificate cert = userCertificateRepository
                .findFirstByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new BadRequestException("No active certificate found for your account"));
        byte[] pkcs12 = cryptoService.decrypt(cert.getEncryptedPkcs12());
        List<SignatureField> fields = signatureFieldRepository.findBySignerId(signer.getId());
        byte[] pdf = documentStorage.readBytes(document.getStoragePath());
        try {
            byte[] signed = pdfCertificateSigner.applyCertificateSignature(
                    pdf, fields, pkcs12, request.pfxPassword().toCharArray(), signer.getName());
            documentStorage.writeBytes(document.getStoragePath(), signed);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }
        markSigned(signer, document, ip, userAgent, "CERTIFICATE_SIGN");
    }

    public byte[] download(String token) throws IOException {
        DocumentSigner signer = requireValidSigner(token);
        Document document = documentRepository
                .findById(signer.getDocumentId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return documentStorage.readBytes(document.getStoragePath());
    }

    private void markSigned(DocumentSigner signer, Document document, String ip, String userAgent, String auditType) {
        signer.setStatus(SignerStatus.SIGNED);
        signer.setSignedAt(Instant.now());
        signer.setIp(ip);
        signer.setUserAgent(userAgent);
        documentSignerRepository.save(signer);
        auditService.record(document.getId(), document.getTenantId(), auditType, signer.getEmail(), null, ip);
        maybeCompleteDocument(document);
    }

    private void maybeCompleteDocument(Document document) {
        List<DocumentSigner> signers = documentSignerRepository.findByDocumentIdOrderBySortOrderAsc(document.getId());
        boolean allSigned = signers.stream().allMatch(s -> s.getStatus() == SignerStatus.SIGNED);
        if (allSigned) {
            document.setStatus(DocumentStatus.COMPLETED);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            auditService.record(document.getId(), document.getTenantId(), "DOCUMENT_COMPLETED", null, null, null);
        }
    }

    private Document loadSignableDocument(DocumentSigner signer) {
        Document document = documentRepository
                .findById(signer.getDocumentId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new BadRequestException("Document is not available for signing");
        }
        return document;
    }

    private DocumentSigner requirePendingSigner(String token) {
        DocumentSigner signer = requireValidSigner(token);
        if (signer.getStatus() != SignerStatus.PENDING) {
            throw new BadRequestException("Signing link is no longer active");
        }
        return signer;
    }

    private DocumentSigner requireValidSigner(String token) {
        DocumentSigner signer = documentSignerRepository
                .findByAccessToken(token)
                .orElseThrow(() -> new NotFoundException("Invalid signing link"));
        if (signer.getTokenExpiresAt() != null && signer.getTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Signing link has expired");
        }
        return signer;
    }
}
