package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Votazione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VotazioneRepository extends JpaRepository<Votazione, Long> {
  List<Votazione> findAllByIdAssemblea(Long idAssemblea);
}
