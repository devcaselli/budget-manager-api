package br.com.casellisoftware.budgetmanager.rest.advice;

import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.BulletNotInWalletException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.user.exception.InvalidCredentialsException;
import br.com.casellisoftware.budgetmanager.domain.user.exception.UserNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardInUseException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAlreadyDeletedException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InvalidInstallmentPatchException;
import br.com.casellisoftware.budgetmanager.domain.installment.InvalidStandaloneInstallmentInputException;
import br.com.casellisoftware.budgetmanager.domain.payment.AmountExceedsRemainingException;
import br.com.casellisoftware.budgetmanager.domain.payment.CurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.WalletMismatchException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerLifecycleChangeNotAllowedException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.CrossOwnerAccessException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareAlreadyActiveForSourceException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareAlreadyRevertedException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareCurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStopNotApplicableException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRatioMismatchException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionAlreadyEndedException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.exception.SubscriptionChargeNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.IllegalWalletStateTransitionException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletEffectiveMonthConflictException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * Central HTTP error contract for the application. Each handler maps a concrete
 * exception type to a concrete {@link HttpStatus} and emits a
 * {@link ProblemDetail} (RFC 7807) body — never a raw {@code Map} or stacktrace.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>No catch of {@code IllegalArgumentException} as 400 — that would mask
 *       programmer errors. A legit 400 must be signaled by a domain exception
 *       dedicated to it.</li>
 *   <li>The generic {@code Exception} fallback is last resort: logs the full
 *       stack with a correlation id and returns a sanitized 500. The
 *       correlation id is echoed back to the client so they can reference it
 *       in a bug report.</li>
 *   <li>Every response carries {@code Content-Type: application/problem+json}
 *       (RFC 7807) and a {@code correlationId} property.</li>
 * </ul>
 *
 * <p>Handler methods return {@code ResponseEntity<ProblemDetail>} rather than
 * raw {@code ProblemDetail}, because the raw-return path does not propagate the
 * status code — it defaults to 200. Wrapping in {@code ResponseEntity} keeps
 * status, content type, and body unambiguous.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID = "correlationId";

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
        problem.setTitle("Authentication failed");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNAUTHORIZED, problem);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
        problem.setTitle("Authentication failed");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNAUTHORIZED, problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "A valid bearer token is required"
        );
        problem.setTitle("Authentication required");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNAUTHORIZED, problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "The authenticated user is not allowed to access this resource"
        );
        problem.setTitle("Access denied");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.FORBIDDEN, problem);
    }

    @ExceptionHandler(CrossOwnerAccessException.class)
    public ResponseEntity<ProblemDetail> handleCrossOwnerAccess(CrossOwnerAccessException ex) {
        log.warn("Cross-owner access denied actorOwnerId={} targetOwnerId={} resourceType={} resourceId={}",
                ex.getActorOwnerId(), ex.getTargetOwnerId(), ex.getResourceType(), ex.getResourceId());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Resource not found"
        );
        problem.setTitle("Resource not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldErrorDetail)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request payload failed validation"
        );
        problem.setTitle("Validation failed");
        problem.setProperty("errors", fieldErrors);
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed"
        );
        problem.setTitle("Malformed request");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(
                        v.getPropertyPath() == null ? "unknown" : v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request parameters failed validation"
        );
        problem.setTitle("Validation failed");
        problem.setProperty("errors", fieldErrors);
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Required request parameter is missing: " + ex.getParameterName()
        );
        problem.setTitle("Missing request parameter");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ProblemDetail> handleDateTimeParse(DateTimeParseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request parameter has an invalid date format"
        );
        problem.setTitle("Invalid request parameter");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(BulletNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBulletNotFound(BulletNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Bullet not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleExpenseNotFound(ExpenseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Expense not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleWalletNotFound(WalletNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Wallet not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePaymentNotFound(PaymentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Payment not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(CreditCardNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCreditCardNotFound(CreditCardNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("CreditCard not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(PayerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePayerNotFound(PayerNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Payer not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(InstallmentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleInstallmentNotFound(InstallmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Installment not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Subscription not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(SubscriptionChargeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleSubscriptionChargeNotFound(SubscriptionChargeNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Subscription charge not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(ShareNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleShareNotFound(ShareNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Share not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(ExtraBudgetNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleExtraBudgetNotFound(ExtraBudgetNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("ExtraBudget not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(ReservedBudgetNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleReservedBudgetNotFound(ReservedBudgetNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("ReservedBudget not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(BulletNotInWalletException.class)
    public ResponseEntity<ProblemDetail> handleBulletNotInWallet(BulletNotInWalletException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Bullet not in wallet");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was modified by another request. Please retry.");
        pd.setTitle("Concurrent modification");
        pd.setProperty(CORRELATION_ID, newCorrelationId());
        // WARN-level message only (no stack trace): 409 is an expected-retry signal, not an error. Stack noise would hide real incidents under retry storms.
        log.warn("Optimistic lock conflict on {}: {}", req.getRequestURI(), ex.getMessage());
        return problemResponse(HttpStatus.CONFLICT, pd);
    }

    @ExceptionHandler({
            AmountExceedsRemainingException.class,
            CurrencyMismatchException.class,
            WalletCurrencyMismatchException.class,
            WalletMismatchException.class,
            ShareRatioMismatchException.class,
            ShareCurrencyMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleDomainRuleViolation(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Domain rule violation");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.sharing.TransientPayerNotAllowedForInstallmentException.class)
    public ResponseEntity<ProblemDetail> handleTransientPayerNotAllowed(
            br.com.casellisoftware.budgetmanager.domain.sharing.TransientPayerNotAllowedForInstallmentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Transient payer not allowed for installment share");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.sharing.SourceInUseByShareException.class)
    public ResponseEntity<ProblemDetail> handleSourceInUseByShare(
            br.com.casellisoftware.budgetmanager.domain.sharing.SourceInUseByShareException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Source in use by active share");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        problem.setProperty("sourceType", ex.getSourceType().name());
        problem.setProperty("sourceId", ex.getSourceId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.payer.PayerInUseByShareException.class)
    public ResponseEntity<ProblemDetail> handlePayerInUseByShare(
            br.com.casellisoftware.budgetmanager.domain.payer.PayerInUseByShareException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Payer in use by active share");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(WalletEffectiveMonthConflictException.class)
    public ResponseEntity<ProblemDetail> handleWalletEffectiveMonthConflict(WalletEffectiveMonthConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Wallet effectiveMonth conflict");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(IllegalWalletStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalWalletStateTransition(IllegalWalletStateTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Illegal wallet state transition");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(CreditCardInUseException.class)
    public ResponseEntity<ProblemDetail> handleCreditCardInUse(CreditCardInUseException ex) {
        log.warn("CreditCard delete blocked expenseIds={} installmentIds={}",
                ex.getExpenseIds(), ex.getInstallmentIds());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("CreditCard in use");
        problem.setProperty("expenseCount", ex.getExpenseCount());
        problem.setProperty("installmentCount", ex.getInstallmentCount());
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(InstallmentAlreadyDeletedException.class)
    public ResponseEntity<ProblemDetail> handleInstallmentAlreadyDeleted(InstallmentAlreadyDeletedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Installment already deleted");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(InvalidStandaloneInstallmentInputException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStandaloneInstallmentInput(InvalidStandaloneInstallmentInputException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid installment input");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(InvalidInstallmentPatchException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInstallmentPatch(InvalidInstallmentPatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid installment patch");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler({
            WalletAllocationExceededException.class,
            BulletInUseException.class,
            SubscriptionAlreadyEndedException.class,
            PayerLifecycleChangeNotAllowedException.class,
            ShareAlreadyRevertedException.class,
            ShareAlreadyActiveForSourceException.class,
            ShareStopNotApplicableException.class
    })
    public ResponseEntity<ProblemDetail> handleDomainConflict(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Domain conflict");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkConflictException.class)
    public ResponseEntity<ProblemDetail> handleReservedBudgetLinkConflict(
            br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Source already linked to another reserved budget");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        problem.setProperty("sourceType", ex.getSourceType().name());
        problem.setProperty("sourceId", ex.getSourceId());
        problem.setProperty("conflictingReservedBudgetId", ex.getConflictingReservedBudgetId());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkCapExceededException.class)
    public ResponseEntity<ProblemDetail> handleReservedBudgetLinkCapExceeded(
            br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkCapExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Reserved budget cap exceeded by linked items");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        problem.setProperty("month", ex.getMonth().toString());
        problem.setProperty("sum", ex.getSum().amount());
        problem.setProperty("ceiling", ex.getCeiling().amount());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleReservedBudgetLinkNotFound(
            br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Reserved budget link not found");
        problem.setProperty(CORRELATION_ID, newCorrelationId());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException ex) {
        String correlationId = newCorrelationId();
        log.error("Data access failure [correlationId={}]", correlationId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Data store is temporarily unavailable"
        );
        problem.setTitle("Service unavailable");
        problem.setProperty(CORRELATION_ID, correlationId);
        return problemResponse(HttpStatus.SERVICE_UNAVAILABLE, problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        String correlationId = newCorrelationId();
        log.error("Unhandled exception [correlationId={}]", correlationId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please reference the correlation id if reporting."
        );
        problem.setTitle("Internal server error");
        problem.setProperty(CORRELATION_ID, correlationId);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }


    private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    private static FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() == null
                ? "invalid"
                : fieldError.getDefaultMessage();
        return new FieldErrorDetail(fieldError.getField(), message);
    }

    private static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Lightweight payload for per-field validation errors. A record — not a
     * {@code Map} — so the JSON shape stays stable and the contract is
     * verifiable by tests.
     */
    public record FieldErrorDetail(String field, String message) {
    }
}
