package br.com.casellisoftware.budgetmanager.application.sync.boundary;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;

public interface GetSyncPreferenceBoundary {

    SyncPreferenceOutput execute(String ownerId);
}
