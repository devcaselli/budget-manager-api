package br.com.casellisoftware.budgetmanager.domain.sharing;

public class ShareAlreadyActiveForSourceException extends RuntimeException {

    public ShareAlreadyActiveForSourceException(ShareSourceType sourceType, String sourceId) {
        super("An active share already exists for source " + sourceType + ":" + sourceId);
    }
}
