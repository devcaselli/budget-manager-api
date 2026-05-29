package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

public interface FindActiveShareBySourceBoundary {

    ShareOutput execute(ShareSourceType sourceType, String sourceId, String ownerId);
}
