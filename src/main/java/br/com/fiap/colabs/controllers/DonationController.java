package br.com.fiap.colabs.controllers;

import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.dtos.DonationReviewRequest;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.services.DonationService;
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
@RequestMapping("/donations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Doações", description = "Registro com triagem por IA, revisão por ponto de coleta e mensageria RabbitMQ")
public class DonationController {

    private final DonationService donationService;

    @GetMapping
    @Operation(summary = "Listar doações paginadas conforme perfil autenticado")
    public ResponseEntity<Page<DonationResponse>> findAll(
            @RequestParam(required = false) DonationStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(donationService.findAll(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes de uma doação por ID")
    public ResponseEntity<DonationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Cadastrar nova doação com triagem automática via Spring AI")
    public ResponseEntity<DonationResponse> create(@Valid @RequestBody DonationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.create(request));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('POINT_MANAGER', 'ADMIN')")
    @Operation(summary = "Aprovar, rejeitar ou marcar entrega de doação (gestor do ponto ou ADMIN)")
    public ResponseEntity<DonationResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody DonationReviewRequest request
    ) {
        return ResponseEntity.ok(donationService.review(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Cancelar doação pendente (cidadão autor ou ADMIN)")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        donationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}