package br.com.casellisoftware.budgetmanager.rest.sharing;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindActiveShareBySourceBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindAllSharesByOwnerBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindShareByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareRequestDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareResponseDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.mappers.ShareRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/shares")
@RequiredArgsConstructor
public class ShareController {

    private final SaveShareBoundary saveShareBoundary;
    private final RevertShareBoundary revertShareBoundary;
    private final FindShareByIdBoundary findShareByIdBoundary;
    private final FindActiveShareBySourceBoundary findActiveShareBySourceBoundary;
    private final FindAllSharesByOwnerBoundary findAllSharesByOwnerBoundary;
    private final ShareRestMapper mapper;

    @PostMapping
    public ResponseEntity<ShareResponseDto> create(@Valid @RequestBody ShareRequestDto request,
                                                   AuthenticatedUser authenticatedUser) {
        ShareOutput output = saveShareBoundary.execute(
                mapper.toInput(request).withOwnerId(authenticatedUser.ownerId()));
        ShareResponseDto response = mapper.toResponse(output);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<Void> revert(@PathVariable String id,
                                       AuthenticatedUser authenticatedUser) {
        revertShareBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShareResponseDto> findById(@PathVariable String id,
                                                     AuthenticatedUser authenticatedUser) {
        ShareOutput output = findShareByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @GetMapping("/active")
    public ResponseEntity<ShareResponseDto> findActiveBySource(@RequestParam ShareSourceType sourceType,
                                                                @RequestParam String sourceId,
                                                                AuthenticatedUser authenticatedUser) {
        ShareOutput output = findActiveShareBySourceBoundary.execute(sourceType, sourceId, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @GetMapping
    public ResponseEntity<List<ShareResponseDto>> findAll(AuthenticatedUser authenticatedUser) {
        List<ShareResponseDto> response = findAllSharesByOwnerBoundary.execute(authenticatedUser.ownerId())
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
