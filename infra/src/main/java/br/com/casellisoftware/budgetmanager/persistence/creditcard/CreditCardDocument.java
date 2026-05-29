package br.com.casellisoftware.budgetmanager.persistence.creditcard;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "credit_card_owner_idx", def = "{'ownerId': 1}"),
        @CompoundIndex(name = "credit_card_owner_name_unique_idx", def = "{'ownerId': 1, 'name': 1}", unique = true),
        @CompoundIndex(name = "credit_card_owner_normalized_labels_idx", def = "{'ownerId': 1, 'normalizedLabels': 1}")
})
public class CreditCardDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String name;

    /** User-supplied labels (raw, as entered). */
    private List<String> labels;

    /** Normalized labels derived from {@code labels} — used for ingest sync matching. */
    private List<String> normalizedLabels;

    public CreditCardDocument(String id, Long version, String name) {
        this(id, br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard.LEGACY_OWNER_ID, version, name, List.of(), List.of());
    }
}
