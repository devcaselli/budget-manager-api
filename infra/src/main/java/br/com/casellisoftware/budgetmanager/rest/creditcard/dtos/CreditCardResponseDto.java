package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import java.util.List;

public record CreditCardResponseDto(String id, String name, List<String> labels) {
}
