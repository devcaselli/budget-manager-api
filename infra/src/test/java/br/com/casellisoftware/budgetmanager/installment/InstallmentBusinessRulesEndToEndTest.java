package br.com.casellisoftware.budgetmanager.installment;

import br.com.casellisoftware.budgetmanager.persistence.bullet.BulletDocument;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.CreditCardDocument;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import br.com.casellisoftware.budgetmanager.persistence.installment.InstallmentDocument;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import br.com.casellisoftware.budgetmanager.persistence.subscription.SubscriptionDocument;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.SubscriptionChargeDocument;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InstallmentBusinessRulesEndToEndTest {

    private static final Class<?>[] DOCUMENT_TYPES = {
            BulletDocument.class,
            CreditCardDocument.class,
            ExpenseDocument.class,
            InstallmentDocument.class,
            PaymentDocument.class,
            SubscriptionChargeDocument.class,
            SubscriptionDocument.class,
            WalletDocument.class
    };

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        for (Class<?> documentType : DOCUMENT_TYPES) {
            mongoOperations.remove(new Query(), documentType);
        }
    }

    @Test
    void smokeFlow_installmentsAffectFutureWallets_andCreditCardDeletionKeepsHistoricalInstallmentReferences() throws Exception {
        String creditCardId = extractId(mockMvc.perform(post("/credit-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nubank"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nubank"))
                .andReturn());

        String walletIdMay = createWallet("Wallet May", "2026-05-01", "2026-05");
        String walletIdJune = createWallet("Wallet June", "2026-06-01", "2026-06");

        String installmentExpenseId = extractId(mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Notebook",
                                  "cost": 6000.00,
                                  "purchaseDate": "2026-05-10",
                                  "walletId": "%s",
                                  "creditCardId": "%s",
                                  "installment": true,
                                  "installmentNumber": 6
                                }
                                """.formatted(walletIdMay, creditCardId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditCardId").value(creditCardId))
                .andReturn());

        mockMvc.perform(get("/wallets/{id}", walletIdMay))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(10000.00));

        mockMvc.perform(get("/wallets/{id}", walletIdJune))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(9000.00));

        String installmentId = extractFirstId(mockMvc.perform(get("/installments/wallet/{walletId}", walletIdMay))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].sourceWalletId").value(walletIdMay))
                        .andReturn());

        mockMvc.perform(get("/installments/wallet/{walletId}", walletIdJune))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(installmentId))
                .andExpect(jsonPath("$.content[0].sourceWalletId").value(walletIdMay));

        mockMvc.perform(delete("/installments/{id}", installmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/wallets/{id}", walletIdJune))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(10000.00));

        mockMvc.perform(delete("/credit-cards/{id}", creditCardId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.expenseCount").value(1))
                .andExpect(jsonPath("$.installmentCount").value(1));
    }

    private String createWallet(String description, String startDate, String effectiveMonth) throws Exception {
        return extractId(mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "%s",
                                  "budget": 10000.00,
                                  "startDate": "%s",
                                  "closedDate": null,
                                  "closed": false,
                                  "effectiveMonth": "%s",
                                  "state": "PRODUCTION"
                                }
                                """.formatted(description, startDate, effectiveMonth)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String extractFirstId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content").get(0).get("id").asText();
    }
}
