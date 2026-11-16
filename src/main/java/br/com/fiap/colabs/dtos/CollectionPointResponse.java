package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.models.CollectionPoint;

public record CollectionPointResponse(
        Long id,
        String name,
        String cnpj,
        String address,
        String phone,
        Boolean active
) {
    public static CollectionPointResponse fromEntity(CollectionPoint point) {
        return new CollectionPointResponse(
                point.getId(),
                point.getName(),
                point.getCnpj(),
                point.getAddress(),
                point.getPhone(),
                point.getActive()
        );
    }
}