package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.CitizenRequest;
import br.com.fiap.colabs.dtos.CitizenResponse;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<CitizenResponse> findAll(Pageable pageable) {
        return citizenRepository.findAll(pageable).map(CitizenResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CitizenResponse findById(Long id) {
        currentUser.requireCitizenAccess(id);
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + id));
        return CitizenResponse.fromEntity(citizen);
    }

    @Transactional
    public CitizenResponse create(CitizenRequest request) {
        if (citizenRepository.existsByCpf(request.cpf())) {
            throw new BusinessConflictException("Já existe um cidadão cadastrado com este CPF");
        }
        Citizen citizen = Citizen.builder()
                .name(request.name())
                .cpf(request.cpf())
                .phone(request.phone())
                .neighborhood(request.neighborhood())
                .build();
        return CitizenResponse.fromEntity(citizenRepository.save(citizen));
    }

    @Transactional
    public CitizenResponse update(Long id, CitizenRequest request) {
        currentUser.requireCitizenAccess(id);
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + id));

        if (!citizen.getCpf().equals(request.cpf()) && citizenRepository.existsByCpf(request.cpf())) {
            throw new BusinessConflictException("Já existe outro cidadão cadastrado com este CPF");
        }

        citizen.setName(request.name());
        citizen.setCpf(request.cpf());
        citizen.setPhone(request.phone());
        citizen.setNeighborhood(request.neighborhood());

        return CitizenResponse.fromEntity(citizenRepository.save(citizen));
    }

    @Transactional
    public void delete(Long id) {
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + id));
        citizenRepository.delete(citizen);
    }
}