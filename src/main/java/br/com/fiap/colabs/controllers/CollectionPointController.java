package br.com.fiap.colabs.controllers;

import br.com.fiap.colabs.dtos.CollectionPointRequest;
import br.com.fiap.colabs.dtos.CollectionPointResponse;
import br.com.fiap.colabs.services.CollectionPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/collection-points")
@RequiredArgsConstructor
@Tag(name = "Pontos de Coleta", description = "Gerenciamento de pontos de coleta e recebimento de doações")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;

    @GetMapping
    @Operation(summary = "Listar pontos de coleta ativos (ADMIN pode passar includeInactive=true)")
    public ResponseEntity<Page<CollectionPointResponse>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(collectionPointService.findAll(includeInactive, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ponto de coleta por ID")
    public ResponseEntity<CollectionPointResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionPointService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cadastrar novo ponto de coleta (ADMIN)")
    public ResponseEntity<CollectionPointResponse> create(@Valid @RequestBody CollectionPointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionPointService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('POINT_MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualizar dados ou status ativo de um ponto de coleta")
    public ResponseEntity<CollectionPointResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CollectionPointRequest request
    ) {
        return ResponseEntity.ok(collectionPointService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('POINT_MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desativar ponto de coleta (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        collectionPointService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}