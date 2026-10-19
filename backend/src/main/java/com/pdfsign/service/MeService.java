package com.pdfsign.service;

import com.pdfsign.domain.User;
import com.pdfsign.domain.UserCertificate;
import com.pdfsign.repository.TenantMemberRepository;
import com.pdfsign.repository.TenantRepository;
import com.pdfsign.repository.UserCertificateRepository;
import com.pdfsign.repository.UserRepository;
import com.pdfsign.security.AuthUser;
import com.pdfsign.web.dto.CertificateMetadataDto;
import com.pdfsign.web.dto.UserSummaryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MeService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final UserCertificateRepository userCertificateRepository;
    private final CertificateParsingService certificateParsingService;
    private final CryptoService cryptoService;

    public MeService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMemberRepository tenantMemberRepository,
            UserCertificateRepository userCertificateRepository,
            CertificateParsingService certificateParsingService,
            CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMemberRepository = tenantMemberRepository;
        this.userCertificateRepository = userCertificateRepository;
        this.certificateParsingService = certificateParsingService;
        this.cryptoService = cryptoService;
    }

    public UserSummaryDto getMe(AuthUser authUser) {
        User user = userRepository.findById(authUser.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));
        var tenant = tenantRepository.findById(authUser.getTenantId()).orElseThrow(() -> new NotFoundException("Tenant not found"));
        boolean hasCert = userCertificateRepository.existsByUserIdAndActiveTrue(user.getId());
        return new UserSummaryDto(user.getId(), user.getName(), user.getEmail(), tenant.getId(), tenant.getName(), hasCert);
    }

    @Transactional
    public CertificateMetadataDto uploadCertificate(AuthUser authUser, MultipartFile file, String password) {
        CertificateParsingService.ParsedCertificate parsed =
                certificateParsingService.parseAndValidate(file, password.toCharArray());
        userCertificateRepository.deactivateAllForUser(authUser.getUserId());
        UserCertificate cert = new UserCertificate(
                UUID.randomUUID(),
                authUser.getUserId(),
                cryptoService.encrypt(parsed.pkcs12Bytes()),
                parsed.cn(),
                parsed.validFrom(),
                parsed.validTo(),
                parsed.thumbprint(),
                true);
        userCertificateRepository.save(cert);
        return toDto(cert);
    }

    public List<CertificateMetadataDto> listCertificates(AuthUser authUser) {
        return userCertificateRepository.findByUserIdAndActiveTrueOrderByValidToDesc(authUser.getUserId()).stream()
                .map(MeService::toDto)
                .toList();
    }

    @Transactional
    public void deleteCertificate(AuthUser authUser, UUID certificateId) {
        UserCertificate cert = userCertificateRepository
                .findByIdAndUserId(certificateId, authUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Certificate not found"));
        cert.setActive(false);
        userCertificateRepository.save(cert);
    }

    private static CertificateMetadataDto toDto(UserCertificate cert) {
        return new CertificateMetadataDto(
                cert.getId(), cert.getCn(), cert.getValidFrom(), cert.getValidTo(), cert.getThumbprint(), cert.isActive());
    }
}
