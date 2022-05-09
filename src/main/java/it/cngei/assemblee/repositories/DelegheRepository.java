package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Delega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DelegheRepository extends JpaRepository<Delega, Long> {
  Optional<Delega> findDelegaByDeleganteAndIdAssemblea(Long delegante, Long idAssemblea);
  Optional<Delega> findDelegaByDelegatoAndIdAssemblea(Long delegato, Long idAssemblea);
}
