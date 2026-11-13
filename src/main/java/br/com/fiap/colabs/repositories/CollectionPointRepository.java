package br.com.fiap.colabs.repositories;

import br.com.fiap.colabs.models.CollectionPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionPointRepository extends JpaRepository<CollectionPoint, Long> {
    boolean existsByCnpj(String cnpj);
    Page<CollectionPoint> findByNeighborhoodContainingIgnoreCase(String neighborhood, Pageable pageable);
}