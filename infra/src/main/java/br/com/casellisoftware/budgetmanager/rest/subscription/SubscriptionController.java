package br.com.casellisoftware.budgetmanager.rest.subscription;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindActiveSubscriptionsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindAllSubscriptionsBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.PagedSubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.mappers.SubscriptionRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

@Validated
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SaveSubscriptionBoundary saveSubscriptionBoundary;
    private final PatchSubscriptionBoundary patchSubscriptionBoundary;
    private final DeleteSubscriptionBoundary deleteSubscriptionBoundary;
    private final FindSubscriptionByIdBoundary findSubscriptionByIdBoundary;
    private final FindAllSubscriptionsBoundary findAllSubscriptionsBoundary;
    private final FindActiveSubscriptionsByMonthBoundary findActiveSubscriptionsByMonthBoundary;
    private final SubscriptionRestMapper mapper;

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> save(@Valid @RequestBody SubscriptionRequestDto request,
                                                        AuthenticatedUser authenticatedUser) {
        SubscriptionOutput output = saveSubscriptionBoundary.execute(mapper.toInput(request).withOwnerId(authenticatedUser.ownerId()));
        SubscriptionResponseDto response = mapper.toResponse(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> patch(@PathVariable String id,
                                                         @Valid @RequestBody SubscriptionPatchRequestDto request,
                                                         AuthenticatedUser authenticatedUser) {
        SubscriptionOutput output = patchSubscriptionBoundary.execute(mapper.toPatchInput(id, request).withOwnerId(authenticatedUser.ownerId()));
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       AuthenticatedUser authenticatedUser) {
        deleteSubscriptionBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> findById(@PathVariable String id,
                                                            AuthenticatedUser authenticatedUser) {
        SubscriptionOutput output = findSubscriptionByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @GetMapping
    public ResponseEntity<PagedSubscriptionResponseDto> findAll(
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "activeAt must use YYYY-MM format")
            String activeAt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            AuthenticatedUser authenticatedUser) {

        if (activeAt != null) {
            List<SubscriptionOutput> activeSubscriptions = findActiveSubscriptionsByMonthBoundary
                    .execute(YearMonth.parse(activeAt), authenticatedUser.ownerId());
            return ResponseEntity.ok(mapper.toPagedResponse(activeSubscriptions));
        }

        PageResult<SubscriptionOutput> subscriptions = findAllSubscriptionsBoundary
                .execute(page, size, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toPagedResponse(subscriptions));
    }
}
