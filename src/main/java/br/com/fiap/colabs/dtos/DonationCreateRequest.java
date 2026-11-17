package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.RecordType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DonationCreateRequest(
        @NotNull(message = "Tipo do registro é obrigatório (DONATION_OFFER ou HELP_REQUEST)")
        RecordType type,

        @NotBlank(message = "Título é obrigatório")
        @Size(min = 3, max = 120, message = "Título deve ter entre 3 e 120 caracteres")
        String title,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 10, max = 500, message = "Descrição deve ter entre 10 e 500 caracteres")
        String description,

        @NotNull(message = "ID do cidadão é obrigatório")
        Long citizenId,

        Long collectionPointId,

        @NotEmpty(message = "Informe pelo menos 1 item para o pedido ou doação")
        @Valid
        List<DonationItemRequest> items
) {}