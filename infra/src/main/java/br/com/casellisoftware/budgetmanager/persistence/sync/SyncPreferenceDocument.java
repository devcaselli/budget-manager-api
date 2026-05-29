package br.com.casellisoftware.budgetmanager.persistence.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sync_preferences")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SyncPreferenceDocument {

    @Id
    private String ownerId;

    @Indexed
    private boolean enabled;
}
