package br.com.casellisoftware.budgetmanager.domain.payer;

public class PayerLifecycleChangeNotAllowedException extends RuntimeException {

    public PayerLifecycleChangeNotAllowedException(String payerId) {
        super("Payer lifecycle change not allowed because payer is linked to an active share: " + payerId);
    }
}
