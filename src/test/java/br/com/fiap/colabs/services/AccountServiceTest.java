package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AccountRequest;
import br.com.fiap.colabs.dtos.AccountResponse;
import br.com.fiap.colabs.dtos.LoginResponse;
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
import br.com.fiap.colabs.security.AuthService;
import br.com.fiap.colabs.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CitizenRepository citizenRepository;
    @Mock
    private CollectionPointRepository collectionPointRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AccountService accountService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldCreateCitizenAccountWithHashedPassword() {
        Citizen citizen = Citizen.builder().id(10L).name("Carlos").cpf("11122233344").phone("11999990000").address("Rua D, 400").build();
        when(accountRepository.existsByEmail("carlos@colabs.org")).thenReturn(false);
        when(citizenRepository.findById(10L)).thenReturn(Optional.of(citizen));
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AccountRequest request = new AccountRequest("carlos@colabs.org", "senha123", Roles.ROLE_CITIZEN, 10L, null);
        AccountResponse response = accountService.create(request);

        assertEquals(1L, response.id());
        assertEquals("carlos@colabs.org", response.email());
        assertEquals(Roles.ROLE_CITIZEN, response.role());
        assertEquals(10L, response.citizenId());
    }

    @Test
    void shouldSelfRegisterCitizenWithAddressAndReturnToken() {
        when(accountRepository.existsByEmail("novo@colabs.org")).thenReturn(false);
        when(citizenRepository.existsByCpf("55566677788")).thenReturn(false);
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> {
            Citizen c = inv.getArgument(0);
            c.setId(25L);
            return c;
        });
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(tokenService.generateToken(any())).thenReturn("jwt-token-mock");
        when(tokenService.getExpirationMs()).thenReturn(86400000L);

        RegisterRequest request = new RegisterRequest(
                AccountType.CITIZEN,
                "novo@colabs.org",
                "senha123",
                "Ana Clara",
                "11977776666",
                "Av. Brasil, 500",
                "555.666.777-88",
                null
        );

        LoginResponse response = authService.register(request);

        assertEquals("jwt-token-mock", response.token());
        assertEquals("ROLE_CITIZEN", response.role());
        assertEquals(25L, response.citizenId());
        assertNull(response.collectionPointId());
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        when(accountRepository.existsByEmail("duplicado@colabs.org")).thenReturn(true);

        AccountRequest request = new AccountRequest("duplicado@colabs.org", "senha123", Roles.ROLE_ADMIN, null, null);
        assertThrows(BusinessConflictException.class, () -> accountService.create(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldCreateCollectionPointManagerAccount() {
        CollectionPoint point = CollectionPoint.builder().id(5L).name("Ponto Norte").build();
        when(accountRepository.existsByEmail("norte@colabs.org")).thenReturn(false);
        when(collectionPointRepository.findById(5L)).thenReturn(Optional.of(point));
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hash");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        AccountRequest request = new AccountRequest("norte@colabs.org", "senha123", Roles.ROLE_POINT_MANAGER, null, 5L);
        AccountResponse response = accountService.create(request);

        assertEquals(Roles.ROLE_POINT_MANAGER, response.role());
        assertEquals(5L, response.collectionPointId());
        assertNull(response.citizenId());
    }
}