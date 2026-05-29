package br.com.casellisoftware.budgetmanager.sync;

import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpensePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

class HttpIngestPendingSourceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private HttpIngestPendingSource source;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("http://ingest-test");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        source = new HttpIngestPendingSource(restClientBuilder.build());
    }

    // ---------- listPending ----------

    @Test
    void listPending_happyPath_mapsItemsCorrectly() {
        server.expect(requestTo("http://ingest-test/expenses/pending?limit=10&offset=0"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Owner-Id", "owner-abc"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "id": "pend-1",
                              "bank": "Nubank",
                              "card_last4": "1234",
                              "card_label": "Nu Black",
                              "amount": "150.00",
                              "currency": "BRL",
                              "merchant": "iFood",
                              "purchase_at": "2026-05-20T18:30:00Z"
                            }
                          ],
                          "total": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        PendingExpensePage page = source.listPending("owner-abc", 10, 0);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);

        PendingExpense item = page.items().get(0);
        assertThat(item.id()).isEqualTo("pend-1");
        assertThat(item.ownerId()).isEqualTo("owner-abc");
        assertThat(item.bank()).isEqualTo("Nubank");
        assertThat(item.cardLast4()).isEqualTo("1234");
        assertThat(item.cardLabel()).isEqualTo("Nu Black");
        assertThat(item.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(item.currency()).isEqualTo("BRL");
        assertThat(item.merchant()).isEqualTo("iFood");

        server.verify();
    }

    @Test
    void listPending_emptyItems_returnsEmptyPage() {
        server.expect(requestTo("http://ingest-test/expenses/pending?limit=5&offset=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"items": [], "total": 0}
                        """, MediaType.APPLICATION_JSON));

        PendingExpensePage page = source.listPending("owner-1", 5, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
        server.verify();
    }

    @Test
    void listPending_nullItemsInResponse_returnsEmptyPage() {
        server.expect(requestTo("http://ingest-test/expenses/pending?limit=10&offset=0"))
                .andRespond(withSuccess("""
                        {"items": null, "total": 0}
                        """, MediaType.APPLICATION_JSON));

        PendingExpensePage page = source.listPending("owner-1", 10, 0);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }

    @Test
    void listPending_multipleItems_mapsAll() {
        server.expect(requestTo("http://ingest-test/expenses/pending?limit=100&offset=0"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "id": "pend-1", "bank": "Itaú", "card_last4": "0001",
                              "card_label": "Itaú Personnalité", "amount": "200.00",
                              "currency": "BRL", "merchant": "Uber", "purchase_at": "2026-05-01T10:00:00Z"
                            },
                            {
                              "id": "pend-2", "bank": "Bradesco", "card_last4": "0002",
                              "card_label": null, "amount": "50.00",
                              "currency": "BRL", "merchant": "Posto Shell", "purchase_at": "2026-05-02T12:00:00Z"
                            }
                          ],
                          "total": 2
                        }
                        """, MediaType.APPLICATION_JSON));

        PendingExpensePage page = source.listPending("owner-x", 100, 0);

        assertThat(page.items()).hasSize(2);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(PendingExpense::id)
                .containsExactly("pend-1", "pend-2");
        assertThat(page.items()).extracting(PendingExpense::ownerId)
                .containsOnly("owner-x");
    }

    @Test
    void listPending_serverError_propagatesException() {
        server.expect(requestTo("http://ingest-test/expenses/pending?limit=10&offset=0"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> source.listPending("owner-1", 10, 0))
                .isInstanceOf(Exception.class);
    }

    // ---------- markConsumed ----------

    @Test
    void markConsumed_happyPath_sendsDeleteWithOwnerHeader() {
        server.expect(requestTo("http://ingest-test/expenses/pend-42"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-Owner-Id", "owner-abc"))
                .andRespond(withNoContent());

        source.markConsumed("owner-abc", "pend-42");

        server.verify();
    }

    @Test
    void markConsumed_serverError_propagatesException() {
        server.expect(requestTo("http://ingest-test/expenses/pend-99"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withServerError());

        assertThatThrownBy(() -> source.markConsumed("owner-1", "pend-99"))
                .isInstanceOf(Exception.class);
    }

    // ---------- constructor guard ----------

    @Test
    void constructor_nullRestClient_throwsNullPointerException() {
        assertThatThrownBy(() -> new HttpIngestPendingSource(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("restClient");
    }
}
