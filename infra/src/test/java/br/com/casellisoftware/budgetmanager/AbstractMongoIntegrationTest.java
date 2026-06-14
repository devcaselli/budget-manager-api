package br.com.casellisoftware.budgetmanager;

import br.com.casellisoftware.budgetmanager.configs.MongoConfiguration;
import br.com.casellisoftware.budgetmanager.persistence.auth.RefreshTokenDocument;
import br.com.casellisoftware.budgetmanager.persistence.auth.RevokedTokenDocument;
import br.com.casellisoftware.budgetmanager.persistence.bullet.BulletDocument;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.CreditCardDocument;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import br.com.casellisoftware.budgetmanager.persistence.installment.InstallmentDocument;
import br.com.casellisoftware.budgetmanager.persistence.payer.PayerDocument;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.ReservedBudgetDocument;
import br.com.casellisoftware.budgetmanager.persistence.subscription.SubscriptionDocument;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.SubscriptionChargeDocument;
import br.com.casellisoftware.budgetmanager.persistence.sync.SyncPreferenceDocument;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfiguration.class)
public abstract class AbstractMongoIntegrationTest {

    private static final Class<?>[] DOCUMENT_TYPES = {
            RefreshTokenDocument.class,
            RevokedTokenDocument.class,
            BulletDocument.class,
            CreditCardDocument.class,
            ExpenseDocument.class,
            InstallmentDocument.class,
            PayerDocument.class,
            PaymentDocument.class,
            ReservedBudgetDocument.class,
            SubscriptionChargeDocument.class,
            SubscriptionDocument.class,
            SyncPreferenceDocument.class,
            WalletDocument.class
    };

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MongoOperations mongoOperations;

    @BeforeEach
    void cleanDatabase() {
        for (Class<?> documentType : DOCUMENT_TYPES) {
            mongoOperations.remove(new Query(), documentType);
        }
    }
}
