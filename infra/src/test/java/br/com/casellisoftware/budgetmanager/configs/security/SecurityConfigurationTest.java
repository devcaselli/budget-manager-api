package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SecurityConfigurationTest.DummyController.class,
        properties = {
                "app.security.enabled=true",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/budgetmanager",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/budgetmanager/protocol/openid-connect/certs",
                "spring.security.oauth2.resourceserver.jwt.audiences=budgetmanager-api"
        }
)
@Import({
        SecurityConfiguration.class,
        AuthenticatedUserArgumentResolver.class,
        AuthenticatedUserResolver.class,
        MdcAuthenticationFilter.class,
        SecurityConfigurationTest.DummyController.class
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void secureEndpoint_withoutToken_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/secure"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void secureEndpoint_preflightFromAngularDefaultOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/secure")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void secureEndpoint_withJwt_injectsAuthenticatedUserFromSubject() throws Exception {
        mockMvc.perform(get("/secure")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("user-123")
                                .audience(List.of("budgetmanager-api")))))
                .andExpect(status().isOk())
                .andExpect(content().string("user-123"));
    }

    /**
     * /auth/register is on the public filter chain which has no oauth2ResourceServer.
     * A stale or malformed Authorization header must be silently ignored — not rejected with 401.
     */
    @Test
    void authRegister_withStaleAuthorizationHeader_isNotRejectedWith401() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer stale-or-invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @RestController
    static class DummyController {

        @GetMapping("/secure")
        String secure(AuthenticatedUser user) {
            return user.ownerId();
        }
    }
}
