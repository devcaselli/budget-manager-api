package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyTransactionPreviewOutput;

import java.time.LocalDate;
import java.util.List;

public interface FindPluggyTransactionsBoundary {

    List<PluggyTransactionPreviewOutput> execute(String ownerId, String itemId, LocalDate from, LocalDate to);
}
