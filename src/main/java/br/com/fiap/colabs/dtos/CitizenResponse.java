package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.models.Citizen;

public record CitizenResponse(
        Long id,
        String name,
        String cpf,
        String phone,
        String neighborhood
) {
    public static CitizenResponse fromEntity(Citizen citizen) {
        return new CitizenResponse(
                citizen.getId(),
                citizen.getName(),
                citizen.getCpf(),
                citizen.getPhone(),
                citizen.getNeighborhood()
        );
    }
}