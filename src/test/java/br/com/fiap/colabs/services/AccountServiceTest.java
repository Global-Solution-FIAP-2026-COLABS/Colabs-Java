package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AccountResponse;
import br.com.fiap.colabs.dtos.CitizenRequest;
import br.com.fiap.colabs.dtos.CitizenResponse;
import br.com.fiap.colabs.dtos.CollectionPointRequest;
import br.com.fiap.colabs.dtos.CollectionPointResponse;
import br.com.fiap.colabs.dtos.RegisterRequest;
import br.com.fiap.colabs.enums.AccountType;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.AccountRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CitizenRepository citizenRepository;
    @Mock private CollectionPointRepository collectionPointRepository;
    @Mock private CitizenService citizenService;
    @Mock private CollectionPointService collectionPointService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenService tokenService;

    @InjectMocks private AccountService accountService;

    private static CitizenRequest citizenData() {
        return new CitizenRequest("Maria Silva", "529.982.247-25", "11987654321", "Centro");
    }

    private static CollectionPointRequest pointData() {
        return new CollectionPointRequest("Ponto Solidário Centro", "11.222.333/0001-81",
                "Rua das Flores, 100", "Centro", "1133334444", true);
    }

    @Test
    void register_citizen_createsProfileAndAccountWithCitizenRole() {
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, citizenData(), null);
        when(accountRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(citizenService.create(any())).thenReturn(new CitizenResponse(10L, "Maria Silva", "529.982.247-25", "11987654321", "Centro"));
        when(citizenRepository.getReferenceById(10L)).thenReturn(Citizen.builder().id(10L).build());
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.register(request);

        assertEquals(Roles.ROLE_CITIZEN, response.role());
        assertEquals(10L, response.citizenId());
        assertNull(response.collectionPointId());
        verify(collectionPointService, never()).create(any());
    }

    @Test
    void register_collectionPoint_createsProfileAndAccountWithManagerRole() {
        RegisterRequest request = new RegisterRequest("ponto@email.com", "senha123", AccountType.COLLECTION_POINT, null, pointData());
        when(accountRepository.existsByEmail("ponto@email.com")).thenReturn(false);
        when(collectionPointService.create(any())).thenReturn(
                new CollectionPointResponse(7L, "Ponto Solidário Centro", "11.222.333/0001-81", "Rua das Flores, 100", "Centro", "1133334444", true));
        when(collectionPointRepository.getReferenceById(7L)).thenReturn(CollectionPoint.builder().id(7L).build());
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.register(request);

        assertEquals(Roles.ROLE_POINT_MANAGER, response.role());
        assertEquals(7L, response.collectionPointId());
        assertNull(response.citizenId());
        verify(citizenService, never()).create(any());
    }

    @Test
    void register_citizenWithoutCitizenData_isRejected() {
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, null, null);
        when(accountRepository.existsByEmail("maria@email.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> accountService.register(request));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void register_withBothProfiles_isRejected() {
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, citizenData(), pointData());
        when(accountRepository.existsByEmail("maria@email.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> accountService.register(request));

        verify(citizenService, never()).create(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void register_duplicatedEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, citizenData(), null);
        when(accountRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> accountService.register(request));

        verify(citizenService, never()).create(any());
    }

    @Test
    void accountType_neverMapsToAdmin() {
        for (AccountType type : AccountType.values()) {
            assertNotEquals(Roles.ROLE_ADMIN, type.getRole());
        }
    }
}