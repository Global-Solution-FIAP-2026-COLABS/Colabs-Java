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
        String cleanCpf = normalizeCpf(request.cpf());
        if (citizenRepository.existsByCpf(cleanCpf)) {
            throw new BusinessConflictException("Já existe um cidadão cadastrado com este CPF");
        }

        Citizen citizen = Citizen.builder()
                .name(request.name())
                .cpf(cleanCpf)
                .phone(request.phone())
                .address(request.address())
                .build();

        return CitizenResponse.fromEntity(citizenRepository.save(citizen));
    }

    @Transactional
    public CitizenResponse update(Long id, CitizenRequest request) {
        currentUser.requireCitizenAccess(id);
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + id));

        String cleanCpf = normalizeCpf(request.cpf());
        if (!citizen.getCpf().equals(cleanCpf) && citizenRepository.existsByCpf(cleanCpf)) {
            throw new BusinessConflictException("CPF já utilizado por outro cidadão");
        }

        citizen.setName(request.name());
        citizen.setCpf(cleanCpf);
        citizen.setPhone(request.phone());
        citizen.setAddress(request.address());

        return CitizenResponse.fromEntity(citizenRepository.save(citizen));
    }

    @Transactional
    public void delete(Long id) {
        currentUser.requireCitizenAccess(id);
        if (!citizenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cidadão não encontrado: " + id);
        }
        citizenRepository.deleteById(id);
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}