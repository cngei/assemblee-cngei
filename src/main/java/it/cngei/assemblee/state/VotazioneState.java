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
public class VotazioneState {
  private final Map<Long, Set<Long>> votanti = new ConcurrentHashMap<>();

  public void setVotante(Long idVotazione, Long idPartecipante) {
    if(!votanti.containsKey(idVotazione)) {
      votanti.put(idVotazione, Collections.synchronizedSet(new HashSet<>()));
    }

    votanti.get(idVotazione).add(idPartecipante);
  }

  public Set<Long> getVotanti(Long idVotazione) {
    if(!votanti.containsKey(idVotazione)) {
      votanti.put(idVotazione, Collections.synchronizedSet(new HashSet<>()));
    }
    return votanti.get(idVotazione);
  }
}
