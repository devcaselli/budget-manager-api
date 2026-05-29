package br.com.casellisoftware.budgetmanager.rest.auth;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthenticateUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenRevocationPort;
import br.com.casellisoftware.budgetmanager.domain.user.exception.UserAlreadyExistsException;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.AuthRequestDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.RefreshRequestDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.RegisterRequestDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.RegisterResponseDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.TokenResponseDto;
import br.com.casellisoftware.budgetmanager.rest.auth.mappers.AuthRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Generic registration response — intentionally identical whether the email was
     * new or already registered. Callers cannot distinguish the two outcomes, which
     * prevents account enumeration (OWASP ASVS V3.2.2).
     */
    private static final RegisterResponseDto REGISTER_RESPONSE =
            new RegisterResponseDto("If this email is not yet registered, your account has been created.");

    private final RegisterUserBoundary registerUserBoundary;
    private final AuthenticateUserBoundary authenticateUserBoundary;
    private final RefreshBoundary refreshBoundary;
    private final TokenRevocationPort tokenRevocationPort;
    private final AuthRestMapper authRestMapper;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto body) {
        try {
            registerUserBoundary.execute(authRestMapper.toRegisterInput(body));
        } catch (UserAlreadyExistsException ignored) {
            // Intentionally swallowed — response is identical to the success path
            // to prevent account enumeration via status code or body differences.
        }
        return ResponseEntity.ok(REGISTER_RESPONSE);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponseDto> token(@Valid @RequestBody AuthRequestDto body) {
        TokenResponseDto response = authRestMapper.toTokenResponse(
                authenticateUserBoundary.execute(authRestMapper.toAuthInput(body))
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes the bearer token supplied in the {@code Authorization} header.
     *
     * <p>Requires a valid, non-expired JWT (protected chain). Once revoked, the same
     * token will be rejected by {@link br.com.casellisoftware.budgetmanager.configs.security.JtiRevocationValidator}
     * on every subsequent request, even before its natural expiry.</p>
     *
     * <p>Tokens without a {@code jti} claim (issued before N-2) are accepted by this
     * endpoint but cannot be revoked — they will continue to work until expiry.</p>
     *
     * @return {@code 204 No Content} — idempotent, safe to call multiple times.
     */
    /**
     * Exchanges a valid refresh token for a new access + refresh token pair.
     *
     * <p>Refresh tokens are single-use and rotated on every call. A consumed or
     * unknown token results in {@code 401}. This endpoint is on the public chain
     * (no bearer token required) — the refresh token itself authenticates the request.</p>
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody RefreshRequestDto body) {
        TokenResponseDto response = authRestMapper.toTokenResponse(
                refreshBoundary.execute(new RefreshTokenInput(body.refreshToken()))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        String jti = jwt.getId();
        if (jti != null && jwt.getExpiresAt() != null) {
            tokenRevocationPort.revoke(jti, jwt.getExpiresAt());
        }
        return ResponseEntity.noContent().build();
    }
}
