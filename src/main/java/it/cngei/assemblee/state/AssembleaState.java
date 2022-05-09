package it.cngei.assemblee.state;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScope
@Component
public class AssembleaState {
  private final Map<Long, Set<Long>> presenti = new ConcurrentHashMap<>();

  public void setPresente(Long idAssemblea, Long idPartecipante) {
    if(!presenti.containsKey(idAssemblea)) {
      presenti.put(idAssemblea, Collections.synchronizedSet(new HashSet<>()));
    }

    presenti.get(idAssemblea).add(idPartecipante);
  }

  public void setAssente(Long idAssemblea, Long idPartecipante) {
    if(!presenti.containsKey(idAssemblea)) {
      return;
    }

    presenti.get(idAssemblea).remove(idPartecipante);
  }

  public Set<Long> getPresenti(Long idAssemblea) {
    if(!presenti.containsKey(idAssemblea)) {
      presenti.put(idAssemblea, Collections.synchronizedSet(new HashSet<>()));
    }
    return presenti.get(idAssemblea);
  }
}
