package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;

public interface RegisterPluggyItemBoundary {

    PluggyConnectionOutput execute(String ownerId, String itemId);
}
