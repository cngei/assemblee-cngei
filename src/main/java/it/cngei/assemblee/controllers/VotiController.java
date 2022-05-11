package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotoEditModel;
import it.cngei.assemblee.entities.Delega;
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
import org.springframework.web.bind.annotation.*;

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

  @ModelAttribute(name = "votoModel")
  public VotoEditModel votoModel() {
    return new VotoEditModel();
  }

  @GetMapping("/{idVotazione}")
  public String getVotazioneView(
      Model model,
      @PathVariable("id") Long id,
      @PathVariable("idVotazione") Long idVotazione,
      Principal principal,
      VotoEditModel votoModel
  ) {
    var me = Utils.getKeycloakUserFromPrincipal(principal);
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(Long.valueOf(me.getPreferredUsername()), id);
    var idProprio = votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? me.getPreferredUsername() : UUID.randomUUID().toString();
    var idDelega = votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? String.valueOf(delega.map(Delega::getDelegante).orElse(-1L)) : UUID.randomUUID().toString();

    votoModel.setIdProprio(idProprio);
    votoModel.setIdDelega(idDelega);

    if (assemblea.isEmpty()) {
      throw new NoSuchElementException();
    } else if (votazione.isEmpty()) {
      return "redirect:/assemblee/" + id;
    } else {
      model.addAllAttributes(Map.of(
          "assemblea", assemblea.get(),
          "votazione", votazione.get(),
          "hasDelega", delega.isPresent(),
          "isPalese", votazione.get().getTipoVotazione() == TipoVotazione.PALESE,
          "idProprio", idProprio,
          "idDelega", delega.isPresent() ? idDelega : -1L,
          "votoModel", votoModel
      ));
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

    if (votazioneState.getVotanti(idVotazione).contains(Long.valueOf(me.getPreferredUsername()))) {
      throw new AccessDeniedException("Hai già votato");
    }
    if (votazione.get().isTerminata()) {
      throw new AccessDeniedException("Votazione conclusa");
    }

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else if (votazione.isEmpty()) {
      return "redirect:/assemblee/" + id;
    } else {
      var inProprio = Voto.builder()
          .id(votoModel.getIdProprio())
          .idVotazione(idVotazione)
          .scelte(parseScelte(votoModel.getInProprio(), votazione.get().getScelte()))
          .build();
      votiRepository.save(inProprio);
      votazioneState.setVotante(idVotazione, Long.valueOf(me.getPreferredUsername()));

      if (delega.isPresent()) {
        var perDelega = Voto.builder()
            .id(votoModel.getIdDelega())
            .idVotazione(idVotazione)
            .scelte(parseScelte(votoModel.getPerDelega(), votazione.get().getScelte()))
            .perDelega(true)
            .build();
        votiRepository.save(perDelega);
        votazioneState.setVotante(idVotazione, delega.get().getDelegante());
      }
      if(votazione.get().getTipoVotazione() == TipoVotazione.PALESE) {
        return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
      } else {
        return "redirect:/assemblea/" + id;
      }
    }
  }

  private Long[] parseScelte(List<String> scelte, String[] opzioni) {
    if(scelte == null) {
      return new Long[]{};
    }
    var opzioniStream = Arrays.stream(opzioni).toList();
    return scelte.stream()
        .map(opzioniStream::indexOf)
        .filter(x -> x >= 0)
        .limit(2)
        .map(Long::valueOf)
        .toArray(Long[]::new);
  }
}
