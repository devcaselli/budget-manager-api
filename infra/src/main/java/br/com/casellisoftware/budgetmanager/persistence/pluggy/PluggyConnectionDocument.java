package br.com.casellisoftware.budgetmanager.persistence.pluggy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "pluggy_connections")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PluggyConnectionDocument {

    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String itemId;

    private String connectorId;
    private String status;
    private List<String> accountIds;
    private Instant createdAt;
    private Instant updatedAt;
}
