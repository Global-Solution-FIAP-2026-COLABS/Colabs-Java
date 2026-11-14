package br.com.fiap.colabs.controllers;

import br.com.fiap.colabs.dtos.CitizenRequest;
import br.com.fiap.colabs.dtos.CitizenResponse;
import br.com.fiap.colabs.services.CitizenService;
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
@RequestMapping("/citizens")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cidadãos", description = "Gerenciamento de perfis de cidadãos doadores")
public class CitizenController {

    private final CitizenService citizenService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar cidadãos paginados (somente ADMIN)")
    public ResponseEntity<Page<CitizenResponse>> findAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(citizenService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Buscar perfil de cidadão por ID")
    public ResponseEntity<CitizenResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(citizenService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Cadastrar novo perfil de cidadão")
    public ResponseEntity<CitizenResponse> create(@Valid @RequestBody CitizenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citizenService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Atualizar perfil de cidadão")
    public ResponseEntity<CitizenResponse> update(@PathVariable Long id, @Valid @RequestBody CitizenRequest request) {
        return ResponseEntity.ok(citizenService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover cidadão (somente ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        citizenService.delete(id);
        return ResponseEntity.noContent().build();
    }
}