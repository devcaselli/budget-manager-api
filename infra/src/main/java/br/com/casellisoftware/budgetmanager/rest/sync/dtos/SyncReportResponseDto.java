package br.com.casellisoftware.budgetmanager.rest.sync.dtos;

public record SyncReportResponseDto(int created, int skipped, int fallback, int errors) {
}
