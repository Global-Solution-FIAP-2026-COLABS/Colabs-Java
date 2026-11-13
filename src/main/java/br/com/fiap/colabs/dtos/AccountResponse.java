package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.models.Account;

public record AccountResponse(
        Long id,
        String email,
        Roles role,
        Long citizenId,
        Long collectionPointId
) {
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                account.getCitizen() != null ? account.getCitizen().getId() : null,
                account.getCollectionPoint() != null ? account.getCollectionPoint().getId() : null
        );
    }
}