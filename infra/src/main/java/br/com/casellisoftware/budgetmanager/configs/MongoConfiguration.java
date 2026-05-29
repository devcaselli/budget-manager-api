package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.configs.converters.YearMonthReadConverter;
import br.com.casellisoftware.budgetmanager.configs.converters.YearMonthWriteConverter;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(MongoConfiguration.class);

    @Lazy
    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new YearMonthReadConverter(),
                new YearMonthWriteConverter()
        ));
    }

    /**
     * Creates a partial unique index on {@code (ownerId, sourcePendingId)} for
     * {@link ExpenseDocument}, enforced only when {@code sourcePendingId} is not null.
     *
     * <p>Spring Data's {@code @CompoundIndex(sparse=true)} does not prevent duplicate
     * null values on a compound index because sparse semantics in MongoDB only skip
     * documents where ALL indexed fields are absent — but {@code ownerId} is always
     * present. A partial index with {@code partialFilterExpression} is the correct
     * approach: it only indexes documents where {@code sourcePendingId} exists and
     * is not null, guaranteeing uniqueness for ingest dedup without affecting
     * existing expenses.</p>
     *
     * <p>Idempotent: MongoDB ignores the call if the index already exists with
     * the same spec. Deferred to {@code ContextRefreshedEvent} to avoid a circular
     * dependency during context initialization.</p>
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            String collectionName = mongoTemplate.getCollectionName(ExpenseDocument.class);
            Document keys = new Document("ownerId", 1).append("sourcePendingId", 1);
            Document partialFilter = new Document("sourcePendingId",
                    new Document("$exists", true).append("$ne", null));
            IndexOptions options = new IndexOptions()
                    .name("expense_owner_source_pending_unique_idx")
                    .unique(true)
                    .partialFilterExpression(partialFilter);
            mongoTemplate.getDb().getCollection(collectionName).createIndex(keys, options);
            log.debug("Ensured partial unique index expense_owner_source_pending_unique_idx");
        } catch (Exception e) {
            log.warn("Could not ensure expense_owner_source_pending_unique_idx: {}", e.getMessage());
        }
    }
}
