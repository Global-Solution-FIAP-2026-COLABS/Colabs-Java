package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 60, message = "password must be between 6 and 60 characters")
        String password,

        @NotNull(message = "type is required (CITIZEN or COLLECTION_POINT)")
        AccountType type,

        @Valid
        CitizenRequest citizen,

        @Valid
        CollectionPointRequest collectionPoint
) {}