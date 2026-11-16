package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull(message = "Tipo de conta é obrigatório (CITIZEN ou COLLECTION_POINT)")
        AccountType accountType,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password,

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
        String name,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
        String phone,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(min = 5, max = 200, message = "Endereço deve ter entre 5 e 200 caracteres")
        String address,

        @Pattern(regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$", message = "CPF deve conter 11 dígitos ou formato 000.000.000-00")
        String cpf,

        @Pattern(regexp = "^(\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$", message = "CNPJ deve conter 14 dígitos ou formato 00.000.000/0000-00")
        String cnpj,

        @Size(min = 2, max = 80, message = "Bairro deve ter entre 2 e 80 caracteres")
        String neighborhood
) {}