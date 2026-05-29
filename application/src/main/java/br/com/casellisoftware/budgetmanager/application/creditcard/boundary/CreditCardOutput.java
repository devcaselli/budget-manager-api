package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import java.util.List;

public record CreditCardOutput(String id, String name, List<String> labels) {
}
