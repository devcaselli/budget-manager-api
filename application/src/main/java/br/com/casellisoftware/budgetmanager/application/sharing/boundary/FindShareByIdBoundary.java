package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

public interface FindShareByIdBoundary {

    ShareOutput execute(String shareId, String ownerId);
}
