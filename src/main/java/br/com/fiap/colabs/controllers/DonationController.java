package br.com.fiap.colabs.controllers;

import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.dtos.DonationReviewRequest;
import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Doações e Pedidos", description = "Ofertas de doação, pedidos de ajuda comunitária, triagem por IA e mensageria RabbitMQ")
public class DonationController {

    private final DonationService donationService;

    public record CategoryOption(String value, String label) {}

    @GetMapping("/categories")
    @Operation(summary = "Listar categorias pré-definidas disponíveis (FOOD, CLOTHING, HYGIENE, MEDICINE, FURNITURE, OTHER)")
    public ResponseEntity<List<CategoryOption>> listCategories() {
        List<CategoryOption> categories = Arrays.stream(DonationCategory.values())
                .map(c -> new CategoryOption(c.name(), c.getLabel()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping
    @Operation(summary = "Listar registros próprios ou vinculados ao perfil autenticado")
    public ResponseEntity<Page<DonationResponse>> findAll(
            @RequestParam(required = false) DonationStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(donationService.findAll(status, pageable));
    }

    @GetMapping("/community")
    @Operation(summary = "Mural comunitário: visualizar pedidos de ajuda e ofertas abertas para cidadãos e pontos de coleta")
    public ResponseEntity<Page<DonationResponse>> findCommunityFeed(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) DonationStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(donationService.findCommunityFeed(type, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes e triagem por IA de uma doação ou pedido")
    public ResponseEntity<DonationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Cadastrar oferta de doação (DONATION_OFFER) ou pedido de ajuda (HELP_REQUEST) com triagem via Spring AI")
    public ResponseEntity<DonationResponse> create(@Valid @RequestBody DonationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.create(request));
    }

    @PatchMapping("/{id}/fulfill")
    @PreAuthorize("hasAnyRole('CITIZEN', 'POINT_MANAGER', 'ADMIN')")
    @Operation(summary = "Assumir/atender um pedido de ajuda ou oferta aberta no mural comunitário")
    public ResponseEntity<DonationResponse> fulfill(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.fulfill(id));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('POINT_MANAGER', 'ADMIN')")
    @Operation(summary = "Aprovar, rejeitar ou confirmar entrega (APPROVED, REJECTED, DELIVERED)")
    public ResponseEntity<DonationResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody DonationReviewRequest request
    ) {
        return ResponseEntity.ok(donationService.review(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(summary = "Cancelar doação ou pedido pendente (cidadão autor ou ADMIN)")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        donationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}