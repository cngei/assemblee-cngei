package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Mozione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MozioniRepository extends JpaRepository<Mozione, Long> {
  List<Mozione> findAllByIdAssemblea(Long idAssemblea);
}
