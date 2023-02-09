package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Votazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VotazioneRepository extends JpaRepository<Votazione, Long> {
  @Query("select v from Votazione v where v.idAssemblea = ?1 order by v.id desc")
  List<Votazione> findAllByIdAssemblea(Long idAssemblea);
}
