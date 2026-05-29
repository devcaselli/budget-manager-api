package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import java.util.List;

public interface FindAllPayersBoundary {
    List<PayerOutput> execute(String ownerId);
}
