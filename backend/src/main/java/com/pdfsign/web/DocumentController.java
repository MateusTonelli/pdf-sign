package com.pdfsign.web;

import com.pdfsign.security.SecurityUtils;
import com.pdfsign.service.DocumentService;
import com.pdfsign.web.dto.DocumentDetailDto;
import com.pdfsign.web.dto.DocumentSummaryDto;
import com.pdfsign.web.dto.SignatureFieldInputDto;
import com.pdfsign.web.dto.SignerInputDto;
import com.pdfsign.web.dto.UpdateDocumentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentSummaryDto> list() {
        return documentService.list(SecurityUtils.requireAuthUser());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDetailDto create(
            @RequestParam("file") MultipartFile file, @RequestParam(value = "title", required = false) String title)
            throws IOException {
        return documentService.create(SecurityUtils.requireAuthUser(), file, title);
    }

    @GetMapping("/{id}")
    public DocumentDetailDto get(@PathVariable UUID id) {
        return documentService.get(SecurityUtils.requireAuthUser(), id);
    }

    @PutMapping("/{id}")
    public DocumentDetailDto update(@PathVariable UUID id, @RequestBody UpdateDocumentRequest request) {
        return documentService.update(SecurityUtils.requireAuthUser(), id, request);
    }

    @PutMapping("/{id}/signers")
    public DocumentDetailDto replaceSigners(@PathVariable UUID id, @Valid @RequestBody List<SignerInputDto> signers) {
        return documentService.replaceSigners(SecurityUtils.requireAuthUser(), id, signers);
    }

    @PutMapping("/{id}/fields")
    public DocumentDetailDto replaceFields(
            @PathVariable UUID id, @Valid @RequestBody List<SignatureFieldInputDto> fields) {
        return documentService.replaceFields(SecurityUtils.requireAuthUser(), id, fields);
    }

    @PostMapping("/{id}/send")
    public DocumentDetailDto send(@PathVariable UUID id, HttpServletRequest request) {
        return documentService.send(SecurityUtils.requireAuthUser(), id, clientIp(request));
    }

    @PostMapping("/{id}/cancel")
    public DocumentDetailDto cancel(@PathVariable UUID id, HttpServletRequest request) {
        return documentService.cancel(SecurityUtils.requireAuthUser(), id, clientIp(request));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws IOException {
        byte[] pdf = documentService.download(SecurityUtils.requireAuthUser(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
