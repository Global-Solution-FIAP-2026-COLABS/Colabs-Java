package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.*;
import br.com.fiap.colabs.enums.AccountType;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.AccountRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + id));
        return AccountResponse.fromEntity(account);
    }

    @Transactional
    public AccountResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessConflictException("Já existe uma conta cadastrada com este e-mail");
        }

        Account account;

        if (request.accountType() == AccountType.CITIZEN) {
            if (request.cpf() == null || request.cpf().isBlank()) {
                throw new IllegalArgumentException("CPF é obrigatório para cadastro de cidadão");
            }
            String cleanCpf = request.cpf().replaceAll("\\D", "");
            if (citizenRepository.existsByCpf(cleanCpf)) {
                throw new BusinessConflictException("Já existe um cidadão cadastrado com este CPF");
            }

            Citizen citizen = citizenRepository.save(
                    Citizen.builder()
                            .name(request.name())
                            .cpf(cleanCpf)
                            .phone(request.phone())
                            .address(request.address())
                            .build()
            );

            account = Account.builder()
                    .email(email)
                    .password(passwordEncoder.encode(request.password()))
                    .role(Roles.ROLE_CITIZEN)
                    .citizen(citizen)
                    .build();

        } else if (request.accountType() == AccountType.COLLECTION_POINT) {
            if (request.cnpj() == null || request.cnpj().isBlank()) {
                throw new IllegalArgumentException("CNPJ é obrigatório para cadastro de ponto de coleta");
            }

            String cleanCnpj = request.cnpj().replaceAll("\\D", "");
            if (collectionPointRepository.existsByCnpj(cleanCnpj)) {
                throw new BusinessConflictException("Já existe um ponto de coleta cadastrado com este CNPJ");
            }

            CollectionPoint point = collectionPointRepository.save(
                    CollectionPoint.builder()
                            .name(request.name())
                            .cnpj(cleanCnpj)
                            .address(request.address())
                            .phone(request.phone())
                            .active(true)
                            .build()
            );

            account = Account.builder()
                    .email(email)
                    .password(passwordEncoder.encode(request.password()))
                    .role(Roles.ROLE_POINT_MANAGER)
                    .collectionPoint(point)
                    .build();
        } else {
            throw new IllegalArgumentException("Tipo de conta inválido");
        }

        return AccountResponse.fromEntity(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        String email = request.email().trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessConflictException("Já existe uma conta cadastrada com este e-mail");
        }

        Citizen citizen = null;
        CollectionPoint collectionPoint = null;

        if (request.role() == Roles.ROLE_CITIZEN) {
            if (request.citizenId() == null) {
                throw new IllegalArgumentException("citizenId é obrigatório para ROLE_CITIZEN");
            }
            citizen = citizenRepository.findById(request.citizenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + request.citizenId()));
        } else if (request.role() == Roles.ROLE_POINT_MANAGER) {
            if (request.collectionPointId() == null) {
                throw new IllegalArgumentException("collectionPointId é obrigatório para ROLE_POINT_MANAGER");
            }
            collectionPoint = collectionPointRepository.findById(request.collectionPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + request.collectionPointId()));
        }

        Account account = Account.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .citizen(citizen)
                .collectionPoint(collectionPoint)
                .build();

        return AccountResponse.fromEntity(accountRepository.save(account));
    }

    @Transactional
    public void delete(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conta não encontrada: " + id);
        }
        accountRepository.deleteById(id);
    }


}