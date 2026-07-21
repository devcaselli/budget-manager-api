package br.com.casellisoftware.budgetmanager.pluggy;

import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
                .andExpect(jsonPath("$.itemId").doesNotExist())
                .andRespond(withSuccess("{\"accessToken\":\"connect-token-xyz\"}", MediaType.APPLICATION_JSON));

        ConnectToken token = client.createConnectToken("owner-abc");

        assertThat(token.accessToken()).isEqualTo("connect-token-xyz");
        server.verify();
    }

    @Test
    void createConnectToken_newConnectionOverload_omitsItemId() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andExpect(jsonPath("$.clientUserId").value("owner-abc"))
                .andExpect(jsonPath("$.itemId").doesNotExist())
                .andRespond(withSuccess("{\"accessToken\":\"connect-token-xyz\"}", MediaType.APPLICATION_JSON));

        ConnectToken token = client.createConnectToken("owner-abc", null);

        assertThat(token.accessToken()).isEqualTo("connect-token-xyz");
        server.verify();
    }

    @Test
    void createConnectToken_updateMode_includesItemIdAlongsideClientUserId() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/connect_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andExpect(jsonPath("$.clientUserId").value("owner-abc"))
                .andExpect(jsonPath("$.itemId").value("item-1"))
                .andRespond(withSuccess("{\"accessToken\":\"update-token-xyz\"}", MediaType.APPLICATION_JSON));

        ConnectToken token = client.createConnectToken("owner-abc", "item-1");

        assertThat(token.accessToken()).isEqualTo("update-token-xyz");
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

    // ---------- getItem ----------

    @Test
    void getItem_authenticatesThenReturnsItem() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/items/item-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andRespond(withSuccess(
                        "{\"id\":\"item-1\",\"connector\":{\"id\":\"201\"},\"status\":\"UPDATED\"}",
                        MediaType.APPLICATION_JSON));

        PluggyItem item = client.getItem("item-1");

        assertThat(item.id()).isEqualTo("item-1");
        assertThat(item.connectorId()).isEqualTo("201");
        assertThat(item.status()).isEqualTo("UPDATED");
        server.verify();
    }

    @Test
    void getItem_onUnauthorized_reauthenticatesAndRetriesOnce() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"stale-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/items/item-1"))
                .andExpect(header("X-API-KEY", "stale-key"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"fresh-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/items/item-1"))
                .andExpect(header("X-API-KEY", "fresh-key"))
                .andRespond(withSuccess(
                        "{\"id\":\"item-1\",\"connector\":{\"id\":\"201\"},\"status\":\"UPDATED\"}",
                        MediaType.APPLICATION_JSON));

        PluggyItem item = client.getItem("item-1");

        assertThat(item.status()).isEqualTo("UPDATED");
        server.verify();
    }

    // ---------- listAccounts ----------

    @Test
    void listAccounts_authenticatesThenReturnsAccounts() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(BASE_URL + "/accounts?itemId=item-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andRespond(withSuccess(
                        "{\"results\":[{\"id\":\"acc-1\",\"name\":\"Conta Corrente\",\"type\":\"BANK\"}]}",
                        MediaType.APPLICATION_JSON));

        List<PluggyAccount> accounts = client.listAccounts("item-1");

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).id()).isEqualTo("acc-1");
        assertThat(accounts.get(0).itemId()).isEqualTo("item-1");
        assertThat(accounts.get(0).name()).isEqualTo("Conta Corrente");
        assertThat(accounts.get(0).type()).isEqualTo("BANK");
        server.verify();
    }

    @Test
    void listAccounts_onUnauthorized_reauthenticatesAndRetriesOnce() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"stale-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/accounts?itemId=item-1"))
                .andExpect(header("X-API-KEY", "stale-key"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"fresh-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/accounts?itemId=item-1"))
                .andExpect(header("X-API-KEY", "fresh-key"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        List<PluggyAccount> accounts = client.listAccounts("item-1");

        assertThat(accounts).isEmpty();
        server.verify();
    }

    // ---------- listTransactions (GET /v2/transactions, cursor-based) ----------

    @Test
    void listTransactions_authenticatesThenReturnsTransactions() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(
                        BASE_URL + "/v2/transactions?accountId=acc-1&dateFrom=2026-05-01&dateTo=2026-05-31"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andRespond(withSuccess(
                        "{\"results\":[{\"id\":\"tx-1\",\"description\":\"Uber\",\"amount\":-45.90,"
                                + "\"currencyCode\":\"BRL\",\"date\":\"2026-05-10\",\"type\":\"DEBIT\","
                                + "\"category\":\"Transport\"}],"
                                + "\"next\":null}",
                        MediaType.APPLICATION_JSON));

        List<PluggyTransaction> transactions =
                client.listTransactions("acc-1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(transactions).hasSize(1);
        PluggyTransaction tx = transactions.get(0);
        assertThat(tx.id()).isEqualTo("tx-1");
        assertThat(tx.accountId()).isEqualTo("acc-1");
        assertThat(tx.description()).isEqualTo("Uber");
        assertThat(tx.amount()).isEqualByComparingTo(BigDecimal.valueOf(-45.90));
        assertThat(tx.currency()).isEqualTo("BRL");
        assertThat(tx.date()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(tx.type()).isEqualTo("DEBIT");
        assertThat(tx.category()).isEqualTo("Transport");
        assertThat(tx.isExpense()).isTrue();
        server.verify();
    }

    @Test
    void listTransactions_onUnauthorized_reauthenticatesAndRetriesOnce() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"stale-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(
                        BASE_URL + "/v2/transactions?accountId=acc-1&dateFrom=2026-05-01&dateTo=2026-05-31"))
                .andExpect(header("X-API-KEY", "stale-key"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"fresh-key\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(
                        BASE_URL + "/v2/transactions?accountId=acc-1&dateFrom=2026-05-01&dateTo=2026-05-31"))
                .andExpect(header("X-API-KEY", "fresh-key"))
                .andRespond(withSuccess("{\"results\":[],\"next\":null}", MediaType.APPLICATION_JSON));

        List<PluggyTransaction> transactions =
                client.listTransactions("acc-1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(transactions).isEmpty();
        server.verify();
    }

    @Test
    void listTransactions_followsCursor_drainsAllPagesIntoOneList() {
        server.expect(once(), requestTo(BASE_URL + "/auth"))
                .andRespond(withSuccess("{\"apiKey\":\"jwt-api-key-1\"}", MediaType.APPLICATION_JSON));

        // Page 1: returns a "next" cursor shaped as a verbatim relative query string, per the
        // official docs.pluggy.ai/reference/transactions-list-by-cursor example.
        server.expect(once(), requestTo(
                        BASE_URL + "/v2/transactions?accountId=acc-1&dateFrom=2026-05-01&dateTo=2026-05-31"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andRespond(withSuccess(
                        "{\"results\":[{\"id\":\"tx-1\",\"description\":\"Uber\",\"amount\":-45.90,"
                                + "\"currencyCode\":\"BRL\",\"date\":\"2026-05-10\",\"type\":\"DEBIT\","
                                + "\"category\":\"Transport\"}],"
                                + "\"next\":\"?accountId=acc-1&after=MjAyMC0xMC0xNVQwMDowMDowMC4wMDBafGE4NTM0Yzg1\"}",
                        MediaType.APPLICATION_JSON));

        // Page 2: fetched using the "after" cursor from page 1's "next"; returns next: null.
        server.expect(once(), requestTo(
                        BASE_URL + "/v2/transactions?accountId=acc-1&after=MjAyMC0xMC0xNVQwMDowMDowMC4wMDBafGE4NTM0Yzg1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "jwt-api-key-1"))
                .andRespond(withSuccess(
                        "{\"results\":[{\"id\":\"tx-2\",\"description\":\"iFood\",\"amount\":-30,"
                                + "\"currencyCode\":\"BRL\",\"date\":\"2026-05-11\",\"type\":\"DEBIT\","
                                + "\"category\":\"Restaurants\"}],"
                                + "\"next\":null}",
                        MediaType.APPLICATION_JSON));

        List<PluggyTransaction> transactions =
                client.listTransactions("acc-1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(transactions).extracting(PluggyTransaction::id)
                .containsExactlyInAnyOrder("tx-1", "tx-2");
        server.verify();
    }
}
