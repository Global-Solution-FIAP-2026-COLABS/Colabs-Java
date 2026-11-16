package br.com.fiap.colabs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CitizenRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
        String name,

        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$", message = "CPF deve conter 11 dígitos ou formato 000.000.000-00")
        String cpf,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
        String phone,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(min = 5, max = 200, message = "Endereço deve ter entre 5 e 200 caracteres")
        String address
) {}