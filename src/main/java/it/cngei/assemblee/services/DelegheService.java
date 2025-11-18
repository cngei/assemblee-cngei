package it.cngei.assemblee.services;

import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.state.AssembleaState;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DelegheService {
  private final DelegheRepository delegheRepository;
  private final AssembleeRepository assembleeRepository;
  private final AssembleaState assembleaState;


  public DelegheService(DelegheRepository delegheRepository, AssembleeRepository assembleeRepository, AssembleaState assembleaState) {
    this.delegheRepository = delegheRepository;
    this.assembleeRepository = assembleeRepository;
    this.assembleaState = assembleaState;
  }

  @Cacheable("deleghe")
  public Map<Long, Long> getDeleghe(Long id) {
    return delegheRepository.findAllByIdAssemblea(id).stream()
        .collect(Collectors.toMap(Delega::getDelegante, Delega::getDelegato));
  }

  @Transactional
  @CacheEvict(value = {"deleghe"}, key = "#idAssemblea")
  public void createDelega(Long idAssemblea, Long deleganteId, Long delegatoId) {
    if (deleganteId.equals(delegatoId)) {
      throw new IllegalArgumentException("Non puoi delegare a te stesso");
    }

    var assemblea = assembleeRepository.findById(idAssemblea).orElseThrow(() -> new IllegalArgumentException("Assemblea non trovata"));

    var participants = Arrays.asList(assemblea.getPartecipanti());
    if (!participants.contains(delegatoId) || !participants.contains(deleganteId)) {
      throw new IllegalArgumentException("Il delegato o il delegante non è un partecipante all'assemblea");
    }

    if (delegheRepository.findDelegaByDeleganteAndIdAssemblea(deleganteId, idAssemblea).isPresent()) {
      throw new IllegalStateException("Hai già una delega per questa assemblea");
    }

    if (delegheRepository.findDelegaByDelegatoAndIdAssemblea(delegatoId, idAssemblea).isPresent()) {
      throw new IllegalStateException("Questo utente ha già ricevuto una delega per questa assemblea");
    }

    var newDelega = Delega.builder()
        .idAssemblea(idAssemblea)
        .delegante(deleganteId)
        .delegato(delegatoId)
        .build();

    // Se la persona che delego e' presente, divento presente per delega
    if (assembleaState.getPresenti(idAssemblea).contains(delegatoId)) {
      assembleaState.setPresente(idAssemblea, deleganteId, assemblea.isRequire2FA());
    } else {
      // Altrimenti saro' assente
      assembleaState.setAssente(idAssemblea, deleganteId, assemblea.isRequire2FA());
    }
    delegheRepository.save(newDelega);
  }

  @Transactional
  @CacheEvict(value = {"deleghe"}, key = "#idAssemblea")
  public void deleteDelega(Long idAssemblea, Long deleganteId) {
    var assemblea = assembleeRepository.findById(idAssemblea).orElseThrow(() -> new IllegalArgumentException("Assemblea non trovata"));
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(deleganteId, idAssemblea)
        .orElseThrow(() -> new IllegalStateException("Nessuna delega da annullare"));

    // Se annullo la delega dovro' registrarmi come presente in proprio
    assembleaState.setAssente(idAssemblea, deleganteId, assemblea.isRequire2FA());
    delegheRepository.deleteById(existingDelega.getId());
  }
}
