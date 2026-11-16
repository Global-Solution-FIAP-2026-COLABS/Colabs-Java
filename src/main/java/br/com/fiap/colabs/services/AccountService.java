package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AccountResponse;
import br.com.fiap.colabs.dtos.LoginRequest;
import br.com.fiap.colabs.dtos.LoginResponse;
import br.com.fiap.colabs.dtos.RegisterRequest;
import br.com.fiap.colabs.enums.AccountType;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
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
    private final CitizenService citizenService;
    private final CollectionPointService collectionPointService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;


    @Transactional
    public AccountResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new BusinessConflictException("Já existe uma conta cadastrada com este e-mail");
        }

        Citizen citizen = null;
        CollectionPoint collectionPoint = null;

        if (request.type() == AccountType.CITIZEN) {
            if (request.citizen() == null) {
                throw new IllegalArgumentException("Os dados do cidadão (citizen) são obrigatórios para type=CITIZEN");
            }
            if (request.collectionPoint() != null) {
                throw new IllegalArgumentException("Não envie collectionPoint ao se cadastrar como CITIZEN");
            }
            Long citizenId = citizenService.create(request.citizen()).id();
            citizen = citizenRepository.getReferenceById(citizenId);
        } else {
            if (request.collectionPoint() == null) {
                throw new IllegalArgumentException("Os dados do ponto de coleta (collectionPoint) são obrigatórios para type=COLLECTION_POINT");
            }
            if (request.citizen() != null) {
                throw new IllegalArgumentException("Não envie citizen ao se cadastrar como COLLECTION_POINT");
            }
            Long pointId = collectionPointService.create(request.collectionPoint()).id();
            collectionPoint = collectionPointRepository.getReferenceById(pointId);
        }

        Account account = Account.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.type().getRole())
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