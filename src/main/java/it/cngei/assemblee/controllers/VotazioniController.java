package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotazioneEditModel;
import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.repositories.VotiRepository;
import it.cngei.assemblee.state.AssembleaState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/assemblea/{id}/votazione")
public class VotazioniController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final VotiRepository votiRepository;
  private final AssembleaState assembleaState;

  public VotazioniController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, VotiRepository votiRepository, AssembleaState assembleaState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.votiRepository = votiRepository;
    this.assembleaState = assembleaState;
  }

  @ModelAttribute(name = "votazioneModel")
  public VotazioneEditModel votazioneModel() {
    return new VotazioneEditModel();
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
    scelte.add("Astenuto");
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
    temp.setPresenti((long) assembleaState.getPresenti(id).size());
    votazioneRepository.save(temp);

    return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
  }

  @GetMapping("/{idVotazione}/risultati")
  public String getVotazioneView(Model model, @PathVariable("id") Long id, @PathVariable("idVotazione") Long idVotazione) {
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    var voti = votiRepository.findAllByIdVotazione(idVotazione).stream()
        .peek(x -> x.setId(x.getId().split("-")[0])).collect(Collectors.toList());

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
}
