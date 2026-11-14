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
@Tag(name = "Pontos de Coleta", description = "Gerenciamento de pontos solidários de recebimento")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;

    @GetMapping
    @Operation(summary = "Listar pontos de coleta paginados (com filtro opcional por bairro)")
    public ResponseEntity<Page<CollectionPointResponse>> findAll(
            @RequestParam(required = false) String neighborhood,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(collectionPointService.findAll(neighborhood, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ponto de coleta por ID")
    public ResponseEntity<CollectionPointResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionPointService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cadastrar novo ponto de coleta (somente ADMIN)")
    public ResponseEntity<CollectionPointResponse> create(@Valid @RequestBody CollectionPointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionPointService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('POINT_MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualizar ponto de coleta (gestor vinculado ou ADMIN)")
    public ResponseEntity<CollectionPointResponse> update(@PathVariable Long id, @Valid @RequestBody CollectionPointRequest request) {
        return ResponseEntity.ok(collectionPointService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remover ponto de coleta (somente ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collectionPointService.delete(id);
        return ResponseEntity.noContent().build();
    }
}