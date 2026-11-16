package br.com.fiap.colabs.security;

import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Roles role, Long citizenId, Long pointId) {
        Account account = Account.builder()
                .id(1L)
                .email("user@colabs.com")
                .password("hash")
                .role(role)
                .citizen(citizenId != null ? Citizen.builder().id(citizenId).build() : null)
                .collectionPoint(pointId != null ? CollectionPoint.builder().id(pointId).build() : null)
                .build();
        AccountUserDetails details = new AccountUserDetails(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void getPrincipal_withoutAuthentication_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, currentUser::getPrincipal);
    }

    @Test
    void citizen_canAccessOwnProfile() {
        authenticate(Roles.ROLE_CITIZEN, 10L, null);

        assertDoesNotThrow(() -> currentUser.requireCitizenAccess(10L));
    }

    @Test
    void citizen_cannotAccessAnotherCitizen() {
        authenticate(Roles.ROLE_CITIZEN, 10L, null);

        assertThrows(AccessDeniedException.class, () -> currentUser.requireCitizenAccess(11L));
    }

    @Test
    void admin_canAccessAnyCitizenAndAnyPoint() {
        authenticate(Roles.ROLE_ADMIN, null, null);

        assertTrue(currentUser.isAdmin());
        assertDoesNotThrow(() -> currentUser.requireCitizenAccess(99L));
        assertDoesNotThrow(() -> currentUser.requireCollectionPointAccess(99L));
    }

    @Test
    void manager_canManageOnlyOwnPoint() {
        authenticate(Roles.ROLE_POINT_MANAGER, null, 5L);

        assertDoesNotThrow(() -> currentUser.requireCollectionPointAccess(5L));
        assertThrows(AccessDeniedException.class, () -> currentUser.requireCollectionPointAccess(6L));
    }

    @Test
    void citizen_cannotManageCollectionPoint() {
        authenticate(Roles.ROLE_CITIZEN, 10L, null);

        assertThrows(AccessDeniedException.class, () -> currentUser.requireCollectionPointAccess(5L));
    }
}