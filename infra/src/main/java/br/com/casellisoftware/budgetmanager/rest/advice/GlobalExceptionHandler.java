package br.com.casellisoftware.budgetmanager.rest.advice;

import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.AmountExceedsRemainingException;
import br.com.casellisoftware.budgetmanager.domain.payment.CurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.WalletMismatchException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
            WalletMismatchException.class
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

    @ExceptionHandler({
            WalletAllocationExceededException.class,
            BulletInUseException.class
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
