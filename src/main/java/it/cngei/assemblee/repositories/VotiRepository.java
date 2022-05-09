package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.entities.Voto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VotiRepository extends JpaRepository<Voto, Long> {
  List<Voto> findAllByIdVotazione(Long idVotazione);
}
