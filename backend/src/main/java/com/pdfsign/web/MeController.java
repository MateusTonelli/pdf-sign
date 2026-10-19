package com.pdfsign.web;

import com.pdfsign.security.SecurityUtils;
import com.pdfsign.service.MeService;
import com.pdfsign.web.dto.CertificateMetadataDto;
import com.pdfsign.web.dto.UserSummaryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public UserSummaryDto me() {
        return meService.getMe(SecurityUtils.requireAuthUser());
    }

    @PostMapping("/certificates")
    public CertificateMetadataDto uploadCertificate(
            @RequestParam("file") MultipartFile file, @RequestParam("password") String password) {
        return meService.uploadCertificate(SecurityUtils.requireAuthUser(), file, password);
    }

    @GetMapping("/certificates")
    public List<CertificateMetadataDto> listCertificates() {
        return meService.listCertificates(SecurityUtils.requireAuthUser());
    }

    @DeleteMapping("/certificates/{id}")
    public void deleteCertificate(@PathVariable UUID id) {
        meService.deleteCertificate(SecurityUtils.requireAuthUser(), id);
    }
}
