package br.com.fiap.colabs.services;

import br.com.fiap.colabs.config.EvictCatalogCaches;
import br.com.fiap.colabs.dtos.CollectionPointRequest;
import br.com.fiap.colabs.dtos.CollectionPointResponse;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionPointService {

    private final CollectionPointRepository collectionPointRepository;
    private final CurrentUser currentUser;

    @Cacheable(value = "collectionPoints", key = "(#neighborhood ?: 'all') + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<CollectionPointResponse> findAll(String neighborhood, Pageable pageable) {
        if (neighborhood != null && !neighborhood.isBlank()) {
            return collectionPointRepository.findByNeighborhoodContainingIgnoreCase(neighborhood, pageable)
                    .map(CollectionPointResponse::fromEntity);
        }
        return collectionPointRepository.findAll(pageable).map(CollectionPointResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CollectionPointResponse findById(Long id) {
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));
        return CollectionPointResponse.fromEntity(point);
    }

    @EvictCatalogCaches
    @Transactional
    public CollectionPointResponse create(CollectionPointRequest request) {
        if (collectionPointRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessConflictException("Já existe um ponto de coleta cadastrado com este CNPJ");
        }
        CollectionPoint point = CollectionPoint.builder()
                .name(request.name())
                .cnpj(request.cnpj())
                .address(request.address())
                .phone(request.phone())
                .active(request.active() != null ? request.active() : true)
                .build();
        return CollectionPointResponse.fromEntity(collectionPointRepository.save(point));
    }

    @EvictCatalogCaches
    @Transactional
    public CollectionPointResponse update(Long id, CollectionPointRequest request) {
        currentUser.requireCollectionPointAccess(id);
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));

        if (!point.getCnpj().equals(request.cnpj()) && collectionPointRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessConflictException("Já existe outro ponto de coleta cadastrado com este CNPJ");
        }

        point.setName(request.name());
        point.setCnpj(request.cnpj());
        point.setAddress(request.address());
        point.setPhone(request.phone());
        if (request.active() != null) {
            point.setActive(request.active());
        }

        return CollectionPointResponse.fromEntity(collectionPointRepository.save(point));
    }

    @EvictCatalogCaches
    @Transactional
    public void delete(Long id) {
        CollectionPoint point = collectionPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + id));
        collectionPointRepository.delete(point);
    }
}