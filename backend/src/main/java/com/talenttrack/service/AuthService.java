package com.talenttrack.service;

import com.talenttrack.dto.AuthDtos.*;
import com.talenttrack.entity.CandidateProfile;
import com.talenttrack.entity.RefreshToken;
import com.talenttrack.entity.Role;
import com.talenttrack.entity.User;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.CandidateProfileRepository;
import com.talenttrack.repository.RefreshTokenRepository;
import com.talenttrack.repository.UserRepository;
import com.talenttrack.security.JwtProperties;
import com.talenttrack.security.JwtService;
import com.talenttrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be self-registered");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (req.role() == Role.RECRUITER && (req.companyName() == null || req.companyName().isBlank())) {
            throw ApiException.badRequest("Company name is required for recruiters");
        }
        User user = userRepository.save(User.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .companyName(req.role() == Role.RECRUITER ? req.companyName().trim() : null)
                .enabled(true)
                .build());
        if (user.getRole() == Role.CANDIDATE) {
            profileRepository.save(CandidateProfile.builder().user(user).build());
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.getReferenceById(principal.id());
        return issueTokens(user);
    }

    /** Rotates the refresh token: the old one is revoked and a new pair is issued. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String token) {
        RefreshToken existing = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (!existing.isUsable()) {
            // A revoked token being replayed may indicate theft: revoke every session of the user.
            if (existing.isRevoked()) {
                refreshTokenRepository.revokeAllForUser(existing.getUser());
            }
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        User user = existing.getUser();
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Account is disabled");
        }
        existing.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return userRepository.findById(userId).map(DtoMapper::toUser)
                .orElseThrow(() -> ApiException.notFound("User", userId));
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(UserPrincipal.from(user));
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        RefreshToken refresh = refreshTokenRepository.save(RefreshToken.builder()
                .token(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofDays(jwtProperties.refreshTokenDays())))
                .revoked(false)
                .build());
        return new AuthResponse(access, refresh.getToken(), "Bearer", jwtService.accessTokenTtlSeconds(),
                DtoMapper.toUser(user));
    }
}
