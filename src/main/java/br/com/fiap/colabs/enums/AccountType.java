package br.com.fiap.colabs.enums;

public enum AccountType {
    CITIZEN(Roles.ROLE_CITIZEN),
    COLLECTION_POINT(Roles.ROLE_POINT_MANAGER);

    private final Roles role;

    AccountType(Roles role) {
        this.role = role;
    }

    public Roles getRole() {
        return role;
    }
}