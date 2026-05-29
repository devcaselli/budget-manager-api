package br.com.casellisoftware.budgetmanager.domain.sharing;

public class ShareRatioMismatchException extends RuntimeException {

    public ShareRatioMismatchException(String message) {
        super(message);
    }
}
