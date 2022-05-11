package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.AssembleaEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.entities.Socio;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.state.VotazioneState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assemblea")
public class AssembleaController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final SocioRepository socioRepository;
  private final AssembleaState assembleaState;
  private final VotazioneState votazioneState;

  public AssembleaController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, SocioRepository socioRepository, AssembleaState assembleaState, VotazioneState votazioneState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.socioRepository = socioRepository;
    this.assembleaState = assembleaState;
    this.votazioneState = votazioneState;
  }

  @ModelAttribute(name = "assembleaModel")
  public AssembleaEditModel assembleaModel() {
    return new AssembleaEditModel();
  }

  @GetMapping("/{id}")
  public String getAssemblea(Model model, @PathVariable("id") Long id, Principal principal) {
    var assemblea = assembleeRepository.findById(id);
    var votazioni = votazioneRepository.findAllByIdAssemblea(id);
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      model.addAllAttributes(Map.of(
          "assemblea", assemblea.get(),
          "votazioni", votazioni,
          "presenti", assembleaState.getPresenti(id),
          "isPresente", assembleaState.getPresenti(id).contains(idUtente),
          "hasDelega", delegheRepository.findDelegaByDeleganteAndIdAssemblea(idUtente, id).isPresent() ,
          "canStart", !assemblea.get().isInCorso() && assemblea.get().getIdProprietario().equals(idUtente),
          "canStop", assemblea.get().isInCorso() && assemblea.get().getIdProprietario().equals(idUtente),
          "votazioneState", votazioneState,
          "tessera", idUtente,
          "isProprietario", idUtente == assemblea.get().getIdProprietario()

      ));
      return "assemblee/view";
    }
  }

  @GetMapping("/{id}/presenza")
  public String togglePresenza(@PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(me, id);
    if(assembleaState.getPresenti(id).contains(me)) {
      assembleaState.setAssente(id, me);
      delega.ifPresent(value -> assembleaState.setAssente(id, value.getDelegante()));
    } else {
      assembleaState.setPresente(id, me);
      delega.ifPresent(value -> assembleaState.setPresente(id, value.getDelegante()));
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/presenti")
  public String getPresenti(@PathVariable("id") Long id, Model model) {
    model.addAttribute("presenti", assembleaState.getPresenti(id).stream().map(x -> Map.entry(x, socioRepository.findById(x).map(Socio::getNome).orElse(x.toString()))).collect(Collectors.toList()));
    model.addAttribute("assembleaId", id);
    return "assemblee/presenti";
  }

  @GetMapping("/{id}/caccia/{idUtente}")
  public String kickPartecipante(@PathVariable("id") Long id, @PathVariable("idUtente") Long idUtente) {
    assembleaState.setAssente(id, idUtente);
    return "redirect:/assemblea/" + id + "/presenti";
  }

  @GetMapping("crea")
  public String getCreateAssemblea(Model model, Principal principal) {
    var me = Utils.getKeycloakUserFromPrincipal(principal);
    List<String> groups = (List<String>) me.getOtherClaims().get("groups");
    var sezione = groups.stream().filter(x -> x.matches("/[\\w\\s']+")).findFirst().map(x -> x.substring(1));
    if(sezione.isPresent()) {
      model.addAttribute("nomeSezione", sezione.get());
      var soci = socioRepository.findBySezione(sezione.get());
      model.addAttribute("soci", soci.stream().map(x -> String.valueOf(x.getId())).collect(Collectors.joining("\n")));
    }
    return "assemblee/create";
  }

  @PostMapping("/crea")
  public String createAssemblea(AssembleaEditModel assembleaModel, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var newAssemblea = Assemblea.builder()
        .idProprietario(me)
        .nome(assembleaModel.getNome())
        .descrizione(assembleaModel.getDescrizione())
        .partecipanti(parsePartecipanti(assembleaModel.getPartecipanti()))
        .convocazione(LocalDateTime.parse(assembleaModel.getDateTime()))
        .stepOdg(0L)
        .odg(Arrays.stream(assembleaModel.getOdg().split("\n")).filter(x -> !x.isBlank()).toArray(String[]::new))
        .build();

    assembleeRepository.save(newAssemblea);
    return "redirect:/";
  }
  private Long[] parsePartecipanti(String original) {
    return Arrays.stream(original.split("\n"))
        .map(String::trim)
        .filter(x -> Pattern.matches("\\d+", x))
        .map(Long::parseLong)
        .toArray(Long[]::new);
  }
}
