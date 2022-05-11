package it.cngei.assemblee.state;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.j256.twofactorauth.TimeBasedOneTimePasswordUtil.generateBase32Secret;

@ApplicationScope
@Component
public class AssembleaState {
  private final Map<Long, Set<Long>> presenti = new ConcurrentHashMap<>();
  private final Map<Long, Map<Long, String>> keys2fa = new ConcurrentHashMap<>();

  public void setPresente(Long idAssemblea, Long idPartecipante) {
    if(!presenti.containsKey(idAssemblea)) {
      presenti.put(idAssemblea, Collections.synchronizedSet(new HashSet<>()));
    }
    if(!keys2fa.containsKey(idAssemblea)) {
      keys2fa.put(idAssemblea, new ConcurrentHashMap<>());
    }

    presenti.get(idAssemblea).add(idPartecipante);
    keys2fa.get(idAssemblea).put(idPartecipante, generateBase32Secret());
  }

  public void setAssente(Long idAssemblea, Long idPartecipante) {
    if(!presenti.containsKey(idAssemblea)) {
      return;
    }

    presenti.get(idAssemblea).remove(idPartecipante);
    if(keys2fa.containsKey(idAssemblea)) {
      keys2fa.get(idAssemblea).remove(idPartecipante);
    }
    if(presenti.get(idAssemblea).isEmpty()) {
      presenti.remove(idAssemblea);
      keys2fa.remove(idAssemblea);
    }
  }

  public String get2faSecret(Long idAssemblea, Long idPartecipante) {
    return keys2fa.get(idAssemblea).get(idPartecipante);
  }

  public Set<Long> getPresenti(Long idAssemblea) {
    if(!presenti.containsKey(idAssemblea)) {
      presenti.put(idAssemblea, Collections.synchronizedSet(new HashSet<>()));
    }
    return presenti.get(idAssemblea);
  }
}
