package br.com.fiap.colabs.security;

import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.models.Account;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AccountUserDetails implements UserDetails {

    private final Long accountId;
    private final String email;
    private final String password;
    private final Roles role;
    private final Long citizenId;
    private final Long collectionPointId;

    public AccountUserDetails(Account account) {
        this.accountId = account.getId();
        this.email = account.getEmail();
        this.password = account.getPassword();
        this.role = account.getRole();
        this.citizenId = account.getCitizen() != null ? account.getCitizen().getId() : null;
        this.collectionPointId = account.getCollectionPoint() != null ? account.getCollectionPoint().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}