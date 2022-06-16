package it.cngei.assemblee.services;

import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.repositories.AssembleeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Service
public class AssembleaService {
  private final AssembleeRepository assembleeRepository;

  public AssembleaService(AssembleeRepository assembleeRepository) {
    this.assembleeRepository = assembleeRepository;
  }

  public Assemblea getAssemblea(Long id) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      throw new NoSuchElementException();
    }
    return maybeAssemblea.get();
  }

  public void stopMozioni(Long id) {
    var assemblea = assembleeRepository.findById(id).get();
    assemblea.setMozioniOpen(false);
    assembleeRepository.save(assemblea);
  }

  public void startMozioni(Long id) {
    var assemblea = assembleeRepository.findById(id).get();
    assemblea.setMozioniOpen(true);
    assembleeRepository.save(assemblea);
  }

  public void checkIsDelegato(Long idAssemblea, Long idUtente) {
    var maybeAssemblea = assembleeRepository.findById(idAssemblea);
    if(maybeAssemblea.isEmpty()) {
      throw new NoSuchElementException();
    }
    if(!Arrays.asList(maybeAssemblea.get().getPartecipanti()).contains(idUtente)) {
      throw new AccessDeniedException("Non sei un delegato di questa assemblea");
    }
  }

  public void checkIsAdmin(Long idAssemblea, Long idUtente) {
    var maybeAssemblea = assembleeRepository.findById(idAssemblea);
    if(maybeAssemblea.isEmpty()) {
      throw new NoSuchElementException();
    }
    var assemblea = maybeAssemblea.get();
    if(!idUtente.equals(assemblea.getIdProprietario()) || !idUtente.equals(assemblea.getIdPresidente())) {
      throw new AccessDeniedException("Non sei un amministratore di questa assemblea");
    }
  }
}
