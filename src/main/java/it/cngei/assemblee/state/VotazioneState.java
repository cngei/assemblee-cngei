package it.cngei.assemblee.state;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Set;

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
