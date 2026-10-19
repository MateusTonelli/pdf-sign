package com.pdfsign.service;

import com.pdfsign.domain.Tenant;
import com.pdfsign.domain.TenantMember;
import com.pdfsign.domain.TenantRole;
import com.pdfsign.domain.User;
import com.pdfsign.repository.TenantMemberRepository;
import com.pdfsign.repository.TenantRepository;
import com.pdfsign.repository.UserCertificateRepository;
import com.pdfsign.repository.UserRepository;
import com.pdfsign.security.JwtService;
import com.pdfsign.web.dto.AuthResponse;
import com.pdfsign.web.dto.LoginRequest;
import com.pdfsign.web.dto.RefreshRequest;
import com.pdfsign.web.dto.RegisterRequest;
import com.pdfsign.web.dto.UserSummaryDto;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final UserCertificateRepository userCertificateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMemberRepository tenantMemberRepository,
            UserCertificateRepository userCertificateRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMemberRepository = tenantMemberRepository;
        this.userCertificateRepository = userCertificateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email already registered");
        }
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User(
                userId,
                request.name().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                now);
        Tenant tenant = new Tenant(tenantId, request.companyName().trim(), now);
        TenantMember member = new TenantMember(UUID.randomUUID(), tenantId, userId, TenantRole.OWNER);
        userRepository.save(user);
        tenantRepository.save(tenant);
        tenantMemberRepository.save(member);
        return buildAuthResponse(user, tenant);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        TenantMember member = tenantMemberRepository
                .findFirstByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no tenant membership"));
        Tenant tenant = tenantRepository
                .findById(member.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        return buildAuthResponse(user, tenant);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            Claims claims = jwtService.parseToken(request.refreshToken());
            if (!jwtService.isRefreshToken(claims)) {
                throw new BadRequestException("Invalid refresh token");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("Invalid refresh token"));
            UUID tenantId = UUID.fromString(claims.get(JwtService.CLAIM_TENANT_ID, String.class));
            Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new BadRequestException("Invalid refresh token"));
            return buildAuthResponse(user, tenant);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid refresh token");
        }
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), tenant.getId());
        String refresh = jwtService.createRefreshToken(user.getId(), user.getEmail(), tenant.getId());
        boolean hasCert = userCertificateRepository.existsByUserIdAndActiveTrue(user.getId());
        UserSummaryDto summary = new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                tenant.getId(),
                tenant.getName(),
                hasCert);
        return new AuthResponse(access, refresh, summary);
    }
}
