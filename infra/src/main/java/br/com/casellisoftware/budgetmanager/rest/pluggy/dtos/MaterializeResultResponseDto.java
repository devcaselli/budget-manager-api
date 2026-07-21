package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

public record MaterializeResultResponseDto(int created, int skipped, int fallback, int errors) {
}
