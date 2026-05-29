package br.com.casellisoftware.budgetmanager.application.payer.boundary;

public interface PatchPayerBoundary {
    PayerOutput execute(String id, PayerPatchInput patch, String ownerId);
}
