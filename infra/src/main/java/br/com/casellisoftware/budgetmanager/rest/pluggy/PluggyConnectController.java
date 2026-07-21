package br.com.casellisoftware.budgetmanager.rest.pluggy;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyConnectionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.GetPluggyItemStatusBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.MaterializePluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.RegisterPluggyItemBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyItemStatusOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyTransactionPreviewOutput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.ConnectTokenRequestDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.ConnectTokenResponseDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.MaterializeRequestDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.MaterializeResultResponseDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.PluggyConnectionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.PluggyItemStatusResponseDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.PluggyTransactionPreviewResponseDto;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.RegisterItemRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the Pluggy bank-sync REST surface to the authenticated frontend: connect-token
 * issuance, item registration (widget callback), connection listing, transaction preview,
 * and selective materialization into {@code Expense} records.
 *
 * <p>Secured by the default authenticated filter chain; {@link AuthenticatedUser} is
 * resolved from the JWT subject. The webhook endpoint (phase 2, public) is not part of
 * this controller yet.</p>
 */
@Validated
@RestController
@RequestMapping("/pluggy")
@RequiredArgsConstructor
public class PluggyConnectController {

    private final CreateConnectTokenBoundary createConnectTokenBoundary;
    private final RegisterPluggyItemBoundary registerPluggyItemBoundary;
    private final FindPluggyConnectionsBoundary findPluggyConnectionsBoundary;
    private final FindPluggyTransactionsBoundary findPluggyTransactionsBoundary;
    private final MaterializePluggyTransactionsBoundary materializePluggyTransactionsBoundary;
    private final GetPluggyItemStatusBoundary getPluggyItemStatusBoundary;

    /**
     * Issues a Pluggy Connect Token. No body / {@code itemId} omitted or null → new-connection
     * flow (unchanged). {@code itemId} present → Connect widget update-mode token, scoped to
     * that already-connected item, after an ownership check (404 if not owned).
     */
    @PostMapping("/connect-token")
    public ResponseEntity<ConnectTokenResponseDto> createConnectToken(
            @RequestBody(required = false) ConnectTokenRequestDto request,
            AuthenticatedUser authenticatedUser) {
        String itemId = request != null ? request.itemId() : null;
        ConnectTokenOutput output = createConnectTokenBoundary.execute(authenticatedUser.ownerId(), itemId);
        return ResponseEntity.ok(new ConnectTokenResponseDto(output.connectToken()));
    }

    @PostMapping("/items")
    public ResponseEntity<PluggyConnectionResponseDto> registerItem(
            @Valid @RequestBody RegisterItemRequestDto request,
            AuthenticatedUser authenticatedUser) {
        PluggyConnectionOutput output = registerPluggyItemBoundary.execute(authenticatedUser.ownerId(), request.itemId());
        return ResponseEntity.ok(toResponseDto(output));
    }

    @GetMapping("/connections")
    public ResponseEntity<List<PluggyConnectionResponseDto>> listConnections(AuthenticatedUser authenticatedUser) {
        List<PluggyConnectionResponseDto> response = findPluggyConnectionsBoundary.execute(authenticatedUser.ownerId())
                .stream()
                .map(this::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{itemId}/transactions")
    public ResponseEntity<List<PluggyTransactionPreviewResponseDto>> previewTransactions(
            @PathVariable String itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            AuthenticatedUser authenticatedUser) {
        List<PluggyTransactionPreviewOutput> outputs =
                findPluggyTransactionsBoundary.execute(authenticatedUser.ownerId(), itemId, from, to);
        List<PluggyTransactionPreviewResponseDto> response = outputs.stream()
                .map(o -> new PluggyTransactionPreviewResponseDto(
                        o.id(), o.accountId(), o.description(), o.amount(), o.currency(), o.date(),
                        o.isExpense(), o.alreadyImported()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/{itemId}/materialize")
    public ResponseEntity<MaterializeResultResponseDto> materializeTransactions(
            @PathVariable String itemId,
            @RequestBody MaterializeRequestDto request,
            AuthenticatedUser authenticatedUser) {
        SyncReport report = materializePluggyTransactionsBoundary.execute(
                authenticatedUser.ownerId(), itemId, request.transactionIds(), request.isAll());
        return ResponseEntity.ok(new MaterializeResultResponseDto(
                report.created(), report.skipped(), report.fallback(), report.errors()));
    }

    /**
     * Polling endpoint for the frontend to check an item's sync status after triggering an
     * update-mode re-sync via the Connect widget, until it reaches {@code UPDATED} before
     * re-reading transactions.
     */
    @GetMapping("/items/{itemId}/status")
    public ResponseEntity<PluggyItemStatusResponseDto> getItemStatus(
            @PathVariable String itemId,
            AuthenticatedUser authenticatedUser) {
        PluggyItemStatusOutput output = getPluggyItemStatusBoundary.execute(authenticatedUser.ownerId(), itemId);
        return ResponseEntity.ok(new PluggyItemStatusResponseDto(output.status()));
    }

    private PluggyConnectionResponseDto toResponseDto(PluggyConnectionOutput output) {
        return new PluggyConnectionResponseDto(
                output.id(), output.itemId(), output.connectorId(), output.status(),
                output.accountIds(), output.createdAt(), output.updatedAt());
    }
}
