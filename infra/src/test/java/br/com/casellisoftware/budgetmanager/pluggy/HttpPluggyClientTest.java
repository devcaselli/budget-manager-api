package br.com.casellisoftware.budgetmanager.pluggy;

import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpPluggyClientTest {

    private static final String BASE_URL = "http://pluggy-test";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private HttpPluggyClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new HttpPluggyClient(restClientBuilder.build(), "client-id-x", "client-secret-x");
    }

    @Test
    void createConnectToken_authenticatesThenReturnsAccessToken() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.clientId").value("client-id-x"))
                .andExpect(jsonPath("$.clientSecret").value("client-secret-x"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andExpect(jsonPath("$.clientUserId").value("owner-abc"))
                .andRespond(withSuccess("{\"accessToken\":\"connect-token-xyz\"}", MediaType.APPLICATION_JSON));

        ConnectToken token = client.createConnectToken("owner-abc");

        assertThat(token.accessToken()).isEqualTo("connect-token-xyz");
        server.verify();
    }

    @Test
    void createConnectToken_cachesApiKeyAcrossCalls() {
        // /auth only expected once even though connect_token is called twice
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andRespond(withSuccess("{\"accessToken\":\"tok-1\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andRespond(withSuccess("{\"accessToken\":\"tok-2\"}", MediaType.APPLICATION_JSON));

        assertThat(client.createConnectToken("owner-abc").accessToken()).isEqualTo("tok-1");
        assertThat(client.createConnectToken("owner-abc").accessToken()).isEqualTo("tok-2");
        server.verify();
    }

    @Test
    void createConnectToken_onUnauthorized_reauthenticatesAndRetriesOnce() {
        // initial auth
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"stale-key\"}", MediaType.APPLICATION_JSON));
        // first connect_token rejected -> 401
        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andExpect(header("X-API-KEY", "stale-key"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        // re-auth
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"fresh-key\"}", MediaType.APPLICATION_JSON));
        // retry succeeds
        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andExpect(header("X-API-KEY", "fresh-key"))
                .andRespond(withSuccess("{\"accessToken\":\"tok-after-retry\"}", MediaType.APPLICATION_JSON));

        ConnectToken token = client.createConnectToken("owner-abc");

        assertThat(token.accessToken()).isEqualTo("tok-after-retry");
        server.verify();
    }

    @Test
    void createConnectToken_whenAuthReturnsNoApiKey_throws() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createConnectToken("owner-abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apiKey");
    }
}
