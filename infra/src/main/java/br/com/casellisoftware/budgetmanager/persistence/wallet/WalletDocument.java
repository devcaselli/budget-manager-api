package br.com.casellisoftware.budgetmanager.persistence.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Document
public class WalletDocument {

    @Id
    private String id;

    @Version
    private Long version;

    private String description;
    private Money budget;
    private Money remaining;
    private LocalDate startDate;
    private LocalDate closedDate;
    private Boolean isClosed;

}
