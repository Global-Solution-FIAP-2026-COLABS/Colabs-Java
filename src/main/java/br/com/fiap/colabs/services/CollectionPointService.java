package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.CollectionPointRequest;
import br.com.fiap.colabs.dtos.CollectionPointResponse;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionPointService {

    private final CollectionPointRepository collectionPointRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<CollectionPointResponse> findAll(boolean includeInactive, Pageable pageable) {
        if (includeInactive && currentUser.isAdmin()) {
            return collectionPointRepository.findAll(pageable).map(CollectionPointResponse::fromEntity);
        }
        return collectionPointRepository.findByActiveTrue(pageable).map(CollectionPointResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CollectionPointResponse findById(Long id) {
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));
        return CollectionPointResponse.fromEntity(point);
    }

    @Transactional
    public CollectionPointResponse create(CollectionPointRequest request) {
        String cleanCnpj = normalizeCnpj(request.cnpj());
        if (collectionPointRepository.existsByCnpj(cleanCnpj)) {
            throw new BusinessConflictException("Já existe um ponto de coleta cadastrado com este CNPJ");
        }

        CollectionPoint point = CollectionPoint.builder()
                .name(request.name())
                .cnpj(cleanCnpj)
                .address(request.address())
                .phone(request.phone())
                .active(request.active() != null ? request.active() : true)
                .build();

        return CollectionPointResponse.fromEntity(collectionPointRepository.save(point));
    }

    @Transactional
    public CollectionPointResponse update(Long id, CollectionPointRequest request) {
        currentUser.requireCollectionPointAccess(id);
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));

        String cleanCnpj = normalizeCnpj(request.cnpj());
        if (!point.getCnpj().equals(cleanCnpj) && collectionPointRepository.existsByCnpj(cleanCnpj)) {
            throw new BusinessConflictException("CNPJ já utilizado por outro ponto de coleta");
        }

        point.setName(request.name());
        point.setCnpj(cleanCnpj);
        point.setAddress(request.address());
        point.setPhone(request.phone());
        if (request.active() != null) {
            point.setActive(request.active());
        }

        return CollectionPointResponse.fromEntity(collectionPointRepository.save(point));
    }

    @Transactional
    public void deactivate(Long id) {
        currentUser.requireCollectionPointAccess(id);
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));
        point.setActive(false);
        collectionPointRepository.save(point);
    }

    private String normalizeCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }
}