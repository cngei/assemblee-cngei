package it.cngei.assemblee.services;

import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.state.AssembleaState;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssembleaService {
  private final AssembleeRepository assembleeRepository;
  private final DelegheRepository delegheRepository;
  private final AssembleaState assembleaState;
  private SocioRepository socioRepository;

  public AssembleaService(AssembleeRepository assembleeRepository, DelegheRepository delegheRepository, AssembleaState assembleaState, SocioRepository socioRepository) {
    this.assembleeRepository = assembleeRepository;
    this.delegheRepository = delegheRepository;
    this.assembleaState = assembleaState;
    this.socioRepository = socioRepository;
  }

  public Assemblea getAssemblea(Long id) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      throw new NoSuchElementException();
    }
    return maybeAssemblea.get();
  }

  public void stopMozioni(Long id) {
    var assemblea = getAssemblea(id);
    assemblea.setMozioniOpen(false);
    assembleeRepository.save(assemblea);
  }

  public void startMozioni(Long id) {
    var assemblea = getAssemblea(id);
    assemblea.setMozioniOpen(true);
    assembleeRepository.save(assemblea);
  }

  @Cacheable(value = "partecipanti", key = "#id")
  public List<Map.Entry<Long, String>> getPartecipanti(Long id) {
    var assemblea = getAssemblea(id);
    return Arrays.stream(assemblea.getPartecipanti())
        .map(x -> Map.entry(x, socioRepository.findById(x).map(y -> {
          if(assemblea.isNazionale())
            return y.getSezione() + " - " + y.getNome();
          else return y.getNome();
        }).orElse(x.toString())))
        .sorted(Map.Entry.comparingByValue())
        .collect(Collectors.toList());
  }

  @Cacheable(value = "presentiTotali", key = "#id")
  public Map.Entry<Long, Long> getPresentiTotali(Long id) {
    Set<Long> deleganti = delegheRepository.findAllByIdAssemblea(id).stream().map(Delega::getDelegante).collect(Collectors.toSet());
    Set<Long> presenti = assembleaState.getPresenti(id);
    Long presentiCount = (long) presenti.size();
    presenti.removeIf(deleganti::contains);
    Long inProprioCount = (long) presenti.size();
    return Map.entry(inProprioCount, presentiCount - inProprioCount);
  }

  @Cacheable(value = "isDelegatoCache", key = "{#idAssemblea, #idUtente}")
  public void checkIsDelegato(Long idAssemblea, Long idUtente) {
    var assemblea = getAssemblea(idAssemblea);
    if(!Arrays.asList(assemblea.getPartecipanti()).contains(idUtente)) {
      throw new AccessDeniedException("Non sei un delegato di questa assemblea");
    }
  }

  @Cacheable(value = "isAdminCache", key = "{#idAssemblea, #idUtente}")
  public void checkIsAdmin(Long idAssemblea, Long idUtente) {
    var assemblea = getAssemblea(idAssemblea);
    if(!(Objects.equals(idUtente, assemblea.getIdProprietario()) || Objects.equals(idUtente, assemblea.getIdPresidente()))) {
      throw new AccessDeniedException("Non sei un amministratore di questa assemblea");
    }
  }

  public void nextOdg(Long id) {
      var assemblea = getAssemblea(id);

      if(assemblea.getStepOdg() >= assemblea.getOdg().length) {
        throw new IllegalArgumentException("Non ci sono altri punti all'ordine del giorno");
      }

      assemblea.setStepOdg(assemblea.getStepOdg() + 1);
      assembleeRepository.save(assemblea);
  }

  public void previousOdg(Long id) {
    var assemblea = getAssemblea(id);

    if(assemblea.getStepOdg() <= 0) {
      throw new IllegalArgumentException("Già al primo punto dell'ordine del giorno");
    }

    assemblea.setStepOdg(assemblea.getStepOdg() - 1);
    assembleeRepository.save(assemblea);
  }

  @CacheEvict(value = {"partecipanti", "presentiTotali"}, key = "#id")
  public void addDelegato(Long id, Long tessera) {
    Assemblea assemblea = assembleeRepository.getById(id);
    List<Long> delegati = new ArrayList<>(Arrays.asList(assemblea.getPartecipanti()));
    delegati.add(tessera);
    assemblea.setPartecipanti(delegati.toArray(Long[]::new));
    assembleeRepository.save(assemblea);
  }

  @CacheEvict(value = {"partecipanti", "presentiTotali"}, key = "#id")
  public void removeDelegato(Long id, Long tessera) {
    Assemblea assemblea = assembleeRepository.getById(id);
    List<Long> delegati = new ArrayList<>(Arrays.asList(assemblea.getPartecipanti()));
    delegati.remove(tessera);
    assemblea.setPartecipanti(delegati.toArray(Long[]::new));
    assembleeRepository.save(assemblea);
  }
}
