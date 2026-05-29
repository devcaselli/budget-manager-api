package br.com.casellisoftware.budgetmanager.domain.sharing;

public class ShareNotFoundException extends RuntimeException {

    public ShareNotFoundException(String shareId) {
        super("Share not found: " + shareId);
    }
}
