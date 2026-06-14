package br.com.casellisoftware.budgetmanager.persistence.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Document("reservedBudget")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndex(name = "reservedBudget_owner_active_window_idx", def = "{'ownerId': 1, 'startMonth': 1, 'deleted': 1}")
@CompoundIndex(name = "reservedBudget_links_source_idx", def = "{'links.sourceType': 1, 'links.sourceId': 1, 'ownerId': 1}", sparse = true)
public class ReservedBudgetDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;
    private String details;
    private String currency;
    private FlagEnum flag;

    @Indexed
    private YearMonth startMonth;

    private boolean deleted;
    private LocalDateTime deletedAt;

    private List<ReservedBudgetVersionDocument> versions;

    /** Embedded links; may be {@code null} for legacy documents that pre-date the Vínculos feature. */
    private List<ReservedBudgetLinkDocument> links;
}
