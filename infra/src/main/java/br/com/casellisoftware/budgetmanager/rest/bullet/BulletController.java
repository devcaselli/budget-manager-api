package br.com.casellisoftware.budgetmanager.rest.bullet;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.mappers.BulletRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
    private final BulletRestMapper mapper;

    @PostMapping
    public ResponseEntity<BulletResponseDto> save(@Valid @RequestBody BulletRequestDto request) {
        BulletOutput output = saveBulletBoundary.execute(
                mapper.bulletRequestDtoToBulletInput(request)
        );

        BulletResponseDto response = mapper.bulletOutputToBulletResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletResponseDto> findById(@PathVariable String id) {
        BulletOutput output = findBulletByIdBoundary.execute(id);
        BulletResponseDto response = mapper.bulletOutputToBulletResponseDto(output);

        return ResponseEntity.ok(response);
    }
}
