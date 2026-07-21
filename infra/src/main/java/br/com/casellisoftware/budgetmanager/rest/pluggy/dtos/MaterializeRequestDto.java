package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

import java.util.List;

/**
 * @param transactionIds explicit selection; ignored when {@code all} is {@code true}
 * @param all            when {@code true}, materializes every previewed transaction for the item;
 *                       treated as {@code false} when omitted from the request body
 */
public record MaterializeRequestDto(List<String> transactionIds, Boolean all) {

    /** Null-safe view of {@link #all()} — {@code false} when the field is omitted from the request body. */
    public boolean isAll() {
        return all != null && all;
    }
}
