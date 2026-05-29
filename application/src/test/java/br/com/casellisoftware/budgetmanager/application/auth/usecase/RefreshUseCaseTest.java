package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenData;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import br.com.casellisoftware.budgetmanager.domain.user.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshUseCaseTest {

    private static final String OLD_REFRESH = "old-refresh-uuid";
    private static final String USER_ID     = "user-1";
    private static final String EMAIL       = "user@example.com";
    private static final long   REFRESH_EXP = 604800L;

    @Mock private RefreshTokenPort refreshTokenPort;
    @Mock private TokenGeneratorPort tokenGenerator;

    private RefreshUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshUseCase(refreshTokenPort, tokenGenerator, REFRESH_EXP);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void execute_validRefreshToken_returnsNewTokenPair() {
        when(refreshTokenPort.findByToken(OLD_REFRESH))
                .thenReturn(Optional.of(new RefreshTokenData(USER_ID, EMAIL)));
        when(tokenGenerator.generate(USER_ID, EMAIL))
                .thenReturn(new TokenOutput("new-access", "Bearer", 900, null, 0));

        TokenOutput result = useCase.execute(new RefreshTokenInput(OLD_REFRESH));

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo(OLD_REFRESH);
        assertThat(result.refreshExpiresIn()).isEqualTo(REFRESH_EXP);
    }

    // -----------------------------------------------------------------------
    // Rotation — old token consumed before new one issued
    // -----------------------------------------------------------------------

    @Test
    void execute_validRefreshToken_deletesOldTokenBeforeIssuingNew() {
        when(refreshTokenPort.findByToken(OLD_REFRESH))
                .thenReturn(Optional.of(new RefreshTokenData(USER_ID, EMAIL)));
        when(tokenGenerator.generate(USER_ID, EMAIL))
                .thenReturn(new TokenOutput("access", "Bearer", 900, null, 0));

        useCase.execute(new RefreshTokenInput(OLD_REFRESH));

        verify(refreshTokenPort).delete(OLD_REFRESH);
    }

    @Test
    void execute_validRefreshToken_savesNewRefreshToken() {
        when(refreshTokenPort.findByToken(OLD_REFRESH))
                .thenReturn(Optional.of(new RefreshTokenData(USER_ID, EMAIL)));
        when(tokenGenerator.generate(USER_ID, EMAIL))
                .thenReturn(new TokenOutput("access", "Bearer", 900, null, 0));

        useCase.execute(new RefreshTokenInput(OLD_REFRESH));

        // new token saved with correct userId and email
        verify(refreshTokenPort).save(anyString(), eq(USER_ID), eq(EMAIL), any());
    }

    @Test
    void execute_validRefreshToken_newRefreshTokenIsUuid() {
        when(refreshTokenPort.findByToken(OLD_REFRESH))
                .thenReturn(Optional.of(new RefreshTokenData(USER_ID, EMAIL)));
        when(tokenGenerator.generate(USER_ID, EMAIL))
                .thenReturn(new TokenOutput("access", "Bearer", 900, null, 0));
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        useCase.execute(new RefreshTokenInput(OLD_REFRESH));

        verify(refreshTokenPort).save(tokenCaptor.capture(), any(), any(), any());
        assertThat(java.util.UUID.fromString(tokenCaptor.getValue())).isInstanceOf(java.util.UUID.class);
    }

    // -----------------------------------------------------------------------
    // Invalid / unknown refresh token
    // -----------------------------------------------------------------------

    @Test
    void execute_unknownRefreshToken_throwsInvalidCredentials() {
        when(refreshTokenPort.findByToken(OLD_REFRESH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenInput(OLD_REFRESH)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void execute_unknownRefreshToken_neverIssuesNewToken() {
        when(refreshTokenPort.findByToken(OLD_REFRESH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new RefreshTokenInput(OLD_REFRESH)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenGenerator, never()).generate(any(), any());
        verify(refreshTokenPort, never()).save(any(), any(), any(), any());
        verify(refreshTokenPort, never()).delete(anyString());
    }
}
