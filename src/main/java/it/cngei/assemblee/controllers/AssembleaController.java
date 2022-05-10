package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.NominaPresidenteEditModel;
import it.cngei.assemblee.dtos.OdgEditModel;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.dtos.AssembleaEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.repositories.DelegheRepository;
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
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/assemblea")
public class AssembleaController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final AssembleaState assembleaState;
  private final VotazioneState votazioneState;

  public AssembleaController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, AssembleaState assembleaState, VotazioneState votazioneState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.assembleaState = assembleaState;
    this.votazioneState = votazioneState;
  }

  @ModelAttribute(name = "assembleaModel")
  public AssembleaEditModel assembleaModel() {
    return new AssembleaEditModel();
  }

  @ModelAttribute(name = "odgModel")
  public OdgEditModel odgModel() {
    return new OdgEditModel();
  }

  @ModelAttribute(name = "presidenteModel")
  public NominaPresidenteEditModel presidenteModel() {
    return new NominaPresidenteEditModel();
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
          "tessera", idUtente
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

  @GetMapping("/{id}/inizia")
  public String startAssemblea(@PathVariable("id") Long id, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    if (!assemblea.isInCorso() && assemblea.getFine() == null && (assemblea.getIdProprietario() == idUtente || assemblea.getIdPresidente() == idUtente)) {
      assemblea.setInCorso(true);
      assembleeRepository.save(assemblea);
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/termina")
  public String stopAssemblea(@PathVariable("id") Long id, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    if (assemblea.isInCorso() && (assemblea.getIdProprietario() == idUtente || assemblea.getIdPresidente() == idUtente)) {
      assemblea.setInCorso(false);
      assemblea.setFine(LocalDateTime.now());
      assembleeRepository.save(assemblea);
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("crea")
  public String getCreateAssemblea() {
    return "assemblee/create";
  }

  @PostMapping("/crea")
  public String createAssemblea(AssembleaEditModel assembleaModel) {
    var newAssemblea = Assemblea.builder()
        .idProprietario(1L)
        .nome(assembleaModel.getNome())
        .descrizione(assembleaModel.getDescrizione())
        .partecipanti(parsePartecipanti(assembleaModel.getPartecipanti()))
        .convocazione(LocalDateTime.parse(assembleaModel.getDateTime()))
        .odg(Arrays.stream(assembleaModel.getOdg().split("\n")).filter(x -> !x.isBlank()).toArray(String[]::new))
        .build();

    assembleeRepository.save(newAssemblea);
    return "redirect:/";
  }

  @GetMapping("/{id}/odg")
  public String getEditOdg(@PathVariable("id") Long id, Model model) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    ((OdgEditModel) model.getAttribute("odgModel"))
        .setOdg(String.join("\n", maybeAssemblea.get().getOdg()));

    return "assemblee/editOdg";
  }

  @PostMapping("/{id}/odg")
  public String updateOdg(@PathVariable("id") Long id, OdgEditModel odgEditModel) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    assemblea.setOdg(Arrays.stream(odgEditModel.getOdg().split("\n"))
        .filter(x -> !x.isBlank())
        .map(String::trim)
        .toArray(String[]::new)
    );
    assembleeRepository.save(assemblea);

    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/avanza-odg")
  public String updateOdg(@PathVariable("id") Long id) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    assemblea.setStepOdg(assemblea.getStepOdg() + 1);
    assembleeRepository.save(assemblea);

    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/nomina-presidente")
  public String getNominaPresidente(@PathVariable("id") Long id, Model model) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    model.addAttribute("assemblea", assemblea);
    return "assemblee/nominaPresidente";
  }

  @PostMapping("/{id}/nomina-presidente")
  public String getNominaPresidente(@PathVariable("id") Long id, NominaPresidenteEditModel presidenteEditModel) {
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    // FIXME: controllare che il presidente sia tra i partecipanti
    assemblea.setIdPresidente(presidenteEditModel.getTessera());
    assembleeRepository.save(assemblea);
    return "redirect:/assemblea/" + id;
  }

  private Long[] parsePartecipanti(String original) {
    return Arrays.stream(original.split("\n"))
        .map(String::trim)
        .filter(x -> Pattern.matches("\\d+", x))
        .map(Long::parseLong)
        .toArray(Long[]::new);
  }
}
