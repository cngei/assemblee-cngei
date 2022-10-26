package it.cngei.assemblee.repositories;

import it.cngei.assemblee.entities.Assemblea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssembleeRepository extends JpaRepository<Assemblea, Long> {
  @Query(value = "select * from Assemblea a " +
      "where " +
      "(a.is_nazionale or ?1 = any(a.partecipanti) or ?1 = a.id_proprietario)" +
      "order by a.convocazione desc", nativeQuery = true)
  List<Assemblea> findVisible(Long idUtente);
}
