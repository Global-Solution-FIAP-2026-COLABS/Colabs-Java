package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AccountRequest;
import br.com.fiap.colabs.dtos.AccountResponse;
import br.com.fiap.colabs.dtos.LoginRequest;
import br.com.fiap.colabs.dtos.LoginResponse;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.AccountRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.security.AccountUserDetails;
import br.com.fiap.colabs.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CitizenRepository citizenRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Transactional
    public AccountResponse register(AccountRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new BusinessConflictException("Já existe uma conta cadastrada com este e-mail");
        }

        Citizen citizen = null;
        CollectionPoint collectionPoint = null;

        if (request.role() == Roles.ROLE_CITIZEN) {
            if (request.citizenId() == null) {
                throw new IllegalArgumentException("citizenId é obrigatório para contas ROLE_CITIZEN");
            }
            citizen = citizenRepository.findById(request.citizenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + request.citizenId()));
        } else if (request.role() == Roles.ROLE_POINT_MANAGER) {
            if (request.collectionPointId() == null) {
                throw new IllegalArgumentException("collectionPointId é obrigatório para contas ROLE_POINT_MANAGER");
            }
            collectionPoint = collectionPointRepository.findById(request.collectionPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + request.collectionPointId()));
        }

        Account account = Account.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .citizen(citizen)
                .collectionPoint(collectionPoint)
                .build();

        return AccountResponse.fromEntity(accountRepository.save(account));
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AccountUserDetails user = (AccountUserDetails) auth.getPrincipal();
        String token = tokenService.generateToken(user);

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                user.getCitizenId(),
                user.getCollectionPointId()
        );
    }
}