package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthenticateUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.PasswordEncoderPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.usecase.AuthenticateUserUseCase;
import br.com.casellisoftware.budgetmanager.application.auth.usecase.RegisterUserUseCase;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.persistence.user.UserMongoRepository;
import br.com.casellisoftware.budgetmanager.persistence.user.UserRepositoryImpl;
import br.com.casellisoftware.budgetmanager.persistence.user.mappers.UserPersistenceMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserBeanConfiguration {

    @Bean
    public UserRepository userRepository(UserMongoRepository userMongoRepository,
                                         UserPersistenceMapper userPersistenceMapper) {
        return new UserRepositoryImpl(userMongoRepository, userPersistenceMapper);
    }

    @Bean
    public RegisterUserBoundary registerUserBoundary(UserRepository userRepository,
                                                     PasswordEncoderPort passwordEncoder) {
        return new RegisterUserUseCase(userRepository, passwordEncoder);
    }

    @Bean
    public AuthenticateUserBoundary authenticateUserBoundary(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            TokenGeneratorPort tokenGenerator,
            RefreshTokenPort refreshTokenPort,
            @Value("${app.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        return new AuthenticateUserUseCase(
                userRepository, passwordEncoder, tokenGenerator,
                refreshTokenPort, refreshExpirationSeconds);
    }
}
