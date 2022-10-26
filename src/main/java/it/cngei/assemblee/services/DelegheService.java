package it.cngei.assemblee.services;

import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.DelegheRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DelegheService {
  private final DelegheRepository delegheRepository;

  public DelegheService(DelegheRepository delegheRepository) {
    this.delegheRepository = delegheRepository;
  }

  @Cacheable("deleghe")
  public Map<Long, Long> getDeleghe(Long id) {
    return delegheRepository.findAllByIdAssemblea(id).stream()
        .collect(Collectors.toMap(Delega::getDelegante, Delega::getDelegato));
  }
}
