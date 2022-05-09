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
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/assemblea/{id}/votazione")
public class VotazioniController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final VotiRepository votiRepository;

  public VotazioniController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, VotiRepository votiRepository) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.votiRepository = votiRepository;
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

      if(delega.isPresent()) {
        var perDelega = Voto.builder()
            .id(votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? String.valueOf(delega.get().getDelegante()) : UUID.randomUUID().toString())
            .idVotazione(idVotazione)
            .scelte(parseScelte(votoModel.getPerDelega(), votazione.get().getScelte()))
            .perDelega(true)
            .build();
        votiRepository.save(perDelega);
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
    var orarioFine = votazioneModel.getOrarioFine().isBlank() ? null : LocalDateTime.parse(votazioneModel.getOrarioFine());
    var newVotazione = Votazione.builder()
        .idAssemblea(assemblea.get().getId())
        .orarioFine(orarioFine)
        .quesito(votazioneModel.getQuesito())
        .tipoVotazione(votazioneModel.isVotoPalese() ? TipoVotazione.PALESE : TipoVotazione.SEGRETO)
        .scelte(Arrays.stream(votazioneModel.getScelte().split("\n")).map(String::trim).filter(x -> !x.isBlank()).toArray(String[]::new))
        .terminata(orarioFine != null && orarioFine.isBefore(LocalDateTime.now()))
        .build();
    votazioneRepository.save(newVotazione);

    return "redirect:/assemblea/" + id;
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
