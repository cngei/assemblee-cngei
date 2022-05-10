package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotoEditModel;
import it.cngei.assemblee.entities.Voto;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.repositories.VotiRepository;
import it.cngei.assemblee.state.VotazioneState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/assemblea/{id}/votazione")
public class VotiController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final VotiRepository votiRepository;
  private final VotazioneState votazioneState;

  public VotiController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, VotiRepository votiRepository, VotazioneState votazioneState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.votiRepository = votiRepository;
    this.votazioneState = votazioneState;
  }

  @GetMapping("/{idVotazione}")
  public String getVotazioneView(
      Model model,
      @PathVariable("id") Long id,
      @PathVariable("idVotazione") Long idVotazione,
      Principal principal
  ) {
    var me = Utils.getKeycloakUserFromPrincipal(principal);
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(Long.valueOf(me.getPreferredUsername()), id);

    if (assemblea.isEmpty()) {
      throw new NoSuchElementException();
    } else if (votazione.isEmpty()) {
      return "redirect:/assemblee/" + id;
    } else {
      model.addAllAttributes(Map.of(
          "assemblea", assemblea.get(),
          "votazione", votazione.get(),
          "hasDelega", delega.isPresent()));
      return "votazioni/view";
    }
  }

  @PostMapping("/{idVotazione}")
  public String handleVoto(
      @PathVariable("id") Long id,
      @PathVariable("idVotazione") Long idVotazione,
      VotoEditModel votoModel,
      Principal principal
  ) {
    var me = Utils.getKeycloakUserFromPrincipal(principal);
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(Long.valueOf(me.getPreferredUsername()), id);

    if(votazioneState.getVotanti(idVotazione).contains(Long.valueOf(me.getPreferredUsername()))) {
      throw new AccessDeniedException("Hai già votato");
    }
    if(votazione.get().isTerminata()) {
      throw new AccessDeniedException("Votazione conclusa");
    }

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else if (votazione.isEmpty()) {
      return "redirect:/assemblee/" + id;
    } else {
      var inProprio = Voto.builder()
          .id(votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? me.getPreferredUsername() : UUID.randomUUID().toString())
          .idVotazione(idVotazione)
          .scelte(parseScelte(votoModel.getInProprio(), votazione.get().getScelte()))
          .build();
      votiRepository.save(inProprio);
      votazioneState.setVotante(idVotazione, Long.valueOf(me.getPreferredUsername()));

      if(delega.isPresent()) {
        var perDelega = Voto.builder()
            .id(votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? String.valueOf(delega.get().getDelegante()) : UUID.randomUUID().toString())
            .idVotazione(idVotazione)
            .scelte(parseScelte(votoModel.getPerDelega(), votazione.get().getScelte()))
            .perDelega(true)
            .build();
        votiRepository.save(perDelega);
        votazioneState.setVotante(idVotazione, delega.get().getDelegante());
      }

      return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
    }
  }

  private Long[] parseScelte(List<String> scelte, String[] opzioni) {
    var foo = Arrays.stream(opzioni).toList();
    return scelte.stream()
        .map(foo::indexOf)
        .filter(x -> x >= 0)
        .limit(2)
        .map(Long::valueOf)
        .toArray(Long[]::new);
  }
}
