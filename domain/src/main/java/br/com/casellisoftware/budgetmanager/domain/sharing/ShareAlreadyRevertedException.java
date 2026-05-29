package br.com.casellisoftware.budgetmanager.domain.sharing;

public class ShareAlreadyRevertedException extends RuntimeException {

    public ShareAlreadyRevertedException(String shareId) {
        super("Share already reverted: " + shareId);
    }
}
