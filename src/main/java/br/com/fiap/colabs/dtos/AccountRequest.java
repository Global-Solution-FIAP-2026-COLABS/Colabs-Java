package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.Roles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        @NotBlank(message = "password is requires")
        @Size(min = 6, max = 60, message = "password might be between 6 and 60 characters")
        String password,

        @NotNull(message = "role is required")
        Roles role,

        Long citizenId,
        Long collectionPointId
) {}