package it.cngei.assemblee.state;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScope
@Component
public class VotazioneState {
  @Resource(name = "votiTemplate")
  private SetOperations<Long, Long> voti;

  public void setVotante(Long idVotazione, Long idPartecipante) {
    voti.add(idVotazione, idPartecipante);
  }

  public Set<Long> getVotanti(Long idVotazione) {
    return voti.members(idVotazione);
  }
}
