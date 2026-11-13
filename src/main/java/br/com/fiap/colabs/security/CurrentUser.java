package br.com.fiap.colabs.security;

import br.com.fiap.colabs.enums.Roles;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AccountUserDetails getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AccountUserDetails details)) {
            throw new AccessDeniedException("Usuário não autenticado");
        }
        return details;
    }

    public boolean isAdmin() {
        return getPrincipal().getRole() == Roles.ROLE_ADMIN;
    }

    public void requireCitizenAccess(Long targetCitizenId) {
        AccountUserDetails user = getPrincipal();
        if (user.getRole() == Roles.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() != Roles.ROLE_CITIZEN || user.getCitizenId() == null || !user.getCitizenId().equals(targetCitizenId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar ou alterar dados de outro cidadão");
        }
    }

    public void requireCollectionPointAccess(Long targetCollectionPointId) {
        AccountUserDetails user = getPrincipal();
        if (user.getRole() == Roles.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() != Roles.ROLE_POINT_MANAGER || user.getCollectionPointId() == null
                || !user.getCollectionPointId().equals(targetCollectionPointId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar este ponto de coleta");
        }
    }
}