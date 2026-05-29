package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import java.util.List;

public interface FindAllSharesByOwnerBoundary {

    List<ShareOutput> execute(String ownerId);
}
