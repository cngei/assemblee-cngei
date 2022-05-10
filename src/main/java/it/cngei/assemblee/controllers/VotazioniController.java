package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotazioneEditModel;
import it.cngei.assemblee.dtos.VotoEditModel;
import it.cngei.assemblee.entities.Votazione;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/assemblea/{id}/votazione")
public class VotazioniController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final VotiRepository votiRepository;
  private final VotazioneState votazioneState;

  public VotazioniController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, VotiRepository votiRepository, VotazioneState votazioneState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.votiRepository = votiRepository;
    this.votazioneState = votazioneState;
  }

  @ModelAttribute(name = "votazioneModel")
  public VotazioneEditModel votazioneModel() {
    return new VotazioneEditModel();
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


  @GetMapping("/crea")
  public String getVotazioneView(Model model, @PathVariable("id") Long id) {
    var assemblea = assembleeRepository.findById(id);

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      model.addAttribute("assemblea", assemblea.get());
      return "votazioni/create";
    }
  }

  @PostMapping("/crea")
  public String createVotazione(VotazioneEditModel votazioneModel, @PathVariable("id") Long id) {
    var assemblea = assembleeRepository.findById(id);
    if (assemblea.isEmpty()) {
      return "redirect:/";
    }
    var scelte = Arrays.stream(votazioneModel.getScelte().split("\n")).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toList());
    scelte.add("Scheda bianca");
    scelte.add("Scheda nulla");
    var newVotazione = Votazione.builder()
        .idAssemblea(assemblea.get().getId())
        .quesito(votazioneModel.getQuesito())
        .tipoVotazione(votazioneModel.isVotoPalese() ? TipoVotazione.PALESE : TipoVotazione.SEGRETO)
        .scelte(scelte.toArray(String[]::new))
        .numeroScelte(votazioneModel.getNumeroScelte())
        .terminata(false)
        .build();
    votazioneRepository.save(newVotazione);

    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{idVotazione}/termina")
  public String terminaVotazione(@PathVariable("id") Long id, @PathVariable("idVotazione") Long idVotazione) {
    var assemblea = assembleeRepository.findById(id);
    if (assemblea.isEmpty()) {
      return "redirect:/";
    }
    var votazione = votazioneRepository.findById(idVotazione);
    var temp = votazione.get(); // TODO: rimuovere questo schifo
    temp.setTerminata(true);
    votazioneRepository.save(temp);

    return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
  }

  @GetMapping("/{idVotazione}/risultati")
  public String getVotazioneView(Model model, @PathVariable("id") Long id, @PathVariable("idVotazione") Long idVotazione) {
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    var voti = votiRepository.findAllByIdVotazione(idVotazione);
    var inProprio = IntStream.range(0, votazione.get().getScelte().length)
            .mapToLong(i -> voti.stream().filter(x -> !x.isPerDelega() && Arrays.stream(x.getScelte()).anyMatch(y -> y == i)).count()).toArray();
    var perDelega = IntStream.range(0, votazione.get().getScelte().length)
        .mapToLong(i -> voti.stream().filter(x -> x.isPerDelega() && Arrays.stream(x.getScelte()).anyMatch(y -> y == i)).count()).toArray();

    model.addAllAttributes(Map.of(
        "idAssemblea", id,
        "votazione", votazione.get(),
        "voti", voti,
        "inProprio", inProprio,
        "perDelega", perDelega
    ));

    return "votazioni/risultati";
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
