package br.com.fiap.colabs.repositories;

import br.com.fiap.colabs.models.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    boolean existsByCpf(String cpf);
}