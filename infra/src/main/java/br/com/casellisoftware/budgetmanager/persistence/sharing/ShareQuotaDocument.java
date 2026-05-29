package br.com.casellisoftware.budgetmanager.persistence.sharing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ShareQuotaDocument {

    private String payerId;
    private BigDecimal ratio;
    private List<String> paymentIds;
}
