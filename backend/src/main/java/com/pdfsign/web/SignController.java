package com.pdfsign.web;

import com.pdfsign.service.SignService;
import com.pdfsign.web.dto.CertificateSignRequest;
import com.pdfsign.web.dto.SignSessionDto;
import com.pdfsign.web.dto.SimpleSignRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    private final SignService signService;

    public SignController(SignService signService) {
        this.signService = signService;
    }

    @GetMapping("/{token}")
    public SignSessionDto session(@PathVariable String token) {
        return signService.getSession(token);
    }

    @PostMapping("/{token}/simple")
    public Map<String, String> signSimple(
            @PathVariable String token,
            @RequestBody SimpleSignRequest request,
            HttpServletRequest httpRequest)
            throws IOException {
        signService.signSimple(token, request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return Map.of("message", "Signed successfully");
    }

    @PostMapping("/{token}/certificate")
    public Map<String, String> signCertificate(
            @PathVariable String token,
            @Valid @RequestBody CertificateSignRequest request,
            HttpServletRequest httpRequest)
            throws IOException {
        signService.signCertificate(token, request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return Map.of("message", "Signed successfully");
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<byte[]> download(@PathVariable String token) throws IOException {
        byte[] pdf = signService.download(token);
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
