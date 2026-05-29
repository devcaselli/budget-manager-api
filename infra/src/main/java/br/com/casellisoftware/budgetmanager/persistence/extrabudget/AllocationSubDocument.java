package br.com.casellisoftware.budgetmanager.persistence.extrabudget;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class AllocationSubDocument {

    private String bulletId;
    private BigDecimal amount;
}
