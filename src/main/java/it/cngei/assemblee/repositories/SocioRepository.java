package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SocioRepository extends JpaRepository<Socio, Long> {
  @Query(value = "select * from Socio s where s.sezione = ?1 and s.data_nascita <= (now() - cast('18 years' as interval))", nativeQuery = true)
  List<Socio> findBySezione(String sezione);
}
