package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;

import java.util.List;

public interface FindPluggyConnectionsBoundary {

    List<PluggyConnectionOutput> execute(String ownerId);
}
