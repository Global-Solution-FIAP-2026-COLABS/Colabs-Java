package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.models.Citizen;

public record CitizenResponse(
        Long id,
        String name,
        String cpf,
        String phone,
        String address
) {
    public static CitizenResponse fromEntity(Citizen c) {
        return new CitizenResponse(
                c.getId(),
                c.getName(),
                c.getCpf(),
                c.getPhone(),
                c.getAddress()
        );
    }
}