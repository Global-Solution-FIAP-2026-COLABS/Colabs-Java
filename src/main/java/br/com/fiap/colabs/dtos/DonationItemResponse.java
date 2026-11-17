package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.models.DonationItem;

public record DonationItemResponse(
        Long id,
        String name,
        Integer quantity,
        DonationCategory category,
        String categoryLabel
) {
    public static DonationItemResponse fromEntity(DonationItem item) {
        return new DonationItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getCategory(),
                item.getCategory().getLabel()
        );
    }
}