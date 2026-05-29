package br.com.casellisoftware.budgetmanager.persistence.payer;

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

import java.time.LocalDate;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "payer_owner_deleted_idx", def = "{'ownerId': 1, 'deleted': 1}"),
        @CompoundIndex(name = "payer_owner_wallet_idx", def = "{'ownerId': 1, 'walletId': 1, 'deleted': 1}"),
        @CompoundIndex(name = "payer_owner_type_idx", def = "{'ownerId': 1, 'type': 1, 'deleted': 1}")
})
public class PayerDocument {

    @Id
    private String id;

    private String ownerId;
    private String name;
    private String type;
    private String walletId;
    private String subscriptionId;
    private LocalDate paymentDate;
    private boolean deleted;

    @Version
    private Long version;
}
