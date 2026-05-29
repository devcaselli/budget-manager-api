package br.com.casellisoftware.budgetmanager.rest.bullet;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.mappers.BulletRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Single REST controller for all bullet operations. Each method delegates to
 * the corresponding application-layer use case and uses {@link BulletRestMapper}
 * to translate between HTTP DTOs and application boundary records.
 */
@RestController
@RequestMapping("/bullets")
@RequiredArgsConstructor
public class BulletController {

    private final SaveBulletBoundary saveBulletBoundary;
    private final FindBulletByIdBoundary findBulletByIdBoundary;
    private final FindBulletsByWalletIdBoundary findBulletsByWalletIdBoundary;
    private final PatchBulletBoundary patchBulletBoundary;
    private final DeleteBulletByIdBoundary deleteBulletByIdBoundary;
    private final BulletRestMapper mapper;

    @PostMapping
    public ResponseEntity<BulletResponseDto> save(@Valid @RequestBody BulletRequestDto request,
                                                  AuthenticatedUser authenticatedUser) {
        BulletOutput output = saveBulletBoundary.execute(
                mapper.bulletRequestDtoToBulletInput(request).withOwnerId(authenticatedUser.ownerId())
        );

        BulletResponseDto response = mapper.bulletOutputToBulletResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletResponseDto> findById(@PathVariable String id,
                                                      AuthenticatedUser authenticatedUser) {
        BulletOutput output = findBulletByIdBoundary.execute(id, authenticatedUser.ownerId());
        BulletResponseDto response = mapper.bulletOutputToBulletResponseDto(output);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<BulletResponseDto>> findByWalletId(@PathVariable String walletId,
                                                                  AuthenticatedUser authenticatedUser) {
        List<BulletResponseDto> response = findBulletsByWalletIdBoundary.execute(walletId, authenticatedUser.ownerId())
                .stream()
                .map(mapper::bulletOutputToBulletResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BulletResponseDto> patch(@PathVariable String id,
                                                   @Valid @RequestBody BulletPatchRequestDto request,
                                                   AuthenticatedUser authenticatedUser) {
        BulletOutput output = patchBulletBoundary.execute(
                mapper.bulletPatchRequestDtoToInput(id, request).withOwnerId(authenticatedUser.ownerId())
        );
        BulletResponseDto response = mapper.bulletOutputToBulletResponseDto(output);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       AuthenticatedUser authenticatedUser) {
        deleteBulletByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }
}
