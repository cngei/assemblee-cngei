package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.MessageModel;
import it.cngei.assemblee.dtos.VotazioneEditModel;
import it.cngei.assemblee.entities.Socio;
import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.enums.TipoMessaggio;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.repositories.VotiRepository;
import it.cngei.assemblee.services.AssembleaService;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
  private final SocioRepository socioRepository;
  private final AssembleaState assembleaState;
  private final MessageController messageController;
  private final AssembleaService assembleaService;

  public VotazioniController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, VotiRepository votiRepository, SocioRepository socioRepository, AssembleaState assembleaState, MessageController messageController, AssembleaService assembleaService) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.votiRepository = votiRepository;
    this.socioRepository = socioRepository;
    this.assembleaState = assembleaState;
    this.messageController = messageController;
    this.assembleaService = assembleaService;
  }

  @ModelAttribute(name = "votazioneModel")
  public VotazioneEditModel votazioneModel() {
    return new VotazioneEditModel();
  }

  @GetMapping("/crea")
  public String getVotazioneView(Model model, @PathVariable("id") Long id) {
    var assemblea = assembleaService.getAssemblea(id);
    model.addAttribute("assemblea", assemblea);
    return "votazioni/create";
  }

  @PostMapping("/crea")
  public String createVotazione(VotazioneEditModel votazioneModel, @PathVariable("id") Long id, Principal principal) {
    var idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsAdmin(id, idUtente);
    var scelte = Arrays.stream(votazioneModel.getScelte().split("\n")).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toList());
    scelte.add("Astenuto");
    var newVotazione = Votazione.builder()
        .idAssemblea(id)
        .quesito(votazioneModel.getQuesito())
        .descrizione(votazioneModel.getDescrizione())
        .tipoVotazione(votazioneModel.isVotoPalese() ? TipoVotazione.PALESE : TipoVotazione.SEGRETO)
        .scelte(scelte.toArray(String[]::new))
        .numeroScelte(votazioneModel.getNumeroScelte())
        .quorum(votazioneModel.getQuorum())
        .quorumPerOpzione(votazioneModel.getQuorumPerOpzione())
        .terminata(false)
        .build();
    votazioneRepository.save(newVotazione);
    messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.VOTAZIONE).build());

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
    Map.Entry<Long, Long> presentiTotali = assembleaService.getPresentiTotali(id);
    temp.setPresenti(presentiTotali.getKey() + presentiTotali.getValue());
    votazioneRepository.save(temp);
    messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.VOTAZIONE).build());

    return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
  }

  @GetMapping("/{idVotazione}/risultati")
  public String getVotazioneView(Model model, @PathVariable("id") Long id, @PathVariable("idVotazione") Long idVotazione) {
    var assemblea = assembleeRepository.findById(id);
    var maybeVotazione = votazioneRepository.findById(idVotazione);
    var votazione = maybeVotazione.get();
    var voti = votiRepository.findAllByIdVotazione(idVotazione).stream()
        .peek(x -> x.setId(x.getId().split("-")[0])).collect(Collectors.toList());
    var isPalese = votazione.getTipoVotazione() == TipoVotazione.PALESE;

    var inProprio = IntStream.range(0, votazione.getScelte().length)
        .mapToLong(i -> voti.stream().filter(x -> !x.isPerDelega() && Arrays.stream(x.getScelte()).anyMatch(y -> y == i)).count()).toArray();
    var perDelega = IntStream.range(0, votazione.getScelte().length)
        .mapToLong(i -> voti.stream().filter(x -> x.isPerDelega() && Arrays.stream(x.getScelte()).anyMatch(y -> y == i)).count()).toArray();

    model.addAttribute("quorumRaggiunto", true);
    if (votazione.isTerminata() && votazione.getScelte().length == 3) {
      var maxVoti = IntStream.range(0, votazione.getScelte().length)
          .mapToLong(i -> voti.stream().filter(x -> Arrays.stream(x.getScelte()).anyMatch(y -> y == i)).count()).max().getAsLong();
      if ((double) maxVoti / votazione.getPresenti() <= (votazione.getQuorum() / 100)) {
        model.addAttribute("quorumRaggiunto", false);
      }
    }

    model.addAllAttributes(Map.of(
        "idAssemblea", id,
        "votazione", votazione,
        "voti", voti.stream().peek(x -> {
          if (isPalese) {
            x.setId(socioRepository.findById(Long.valueOf(x.getId())).map(Socio::getNome).orElse(x.getId()));
          }
        }).collect(Collectors.toList()),
        "inProprio", inProprio,
        "perDelega", perDelega
    ));

    return "votazioni/risultati";
  }
}
