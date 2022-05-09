package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.NominaPresidenteEditModel;
import it.cngei.assemblee.dtos.OdgEditModel;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.dtos.AssembleaEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.state.AssembleaState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assemblea")
public class AssembleaController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final AssembleaState assembleaState;

  public AssembleaController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, AssembleaState assembleaState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.assembleaState = assembleaState;
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
  public String getAssemblea(Model model, @PathVariable("id") Long id) {
    var assemblea = assembleeRepository.findById(id);
    var votazioni = votazioneRepository.findAllByIdAssemblea(id);
    var idUtente = 1L;

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      var temp = votazioni.stream().peek(x -> {
        if(x.getOrarioFine() != null && x.getOrarioFine().isBefore(LocalDateTime.now())) {
          x.setTerminata(true);
        }
      }).collect(Collectors.toList());
      model.addAllAttributes(Map.of(
          "assemblea", assemblea.get(),
          "votazioni", temp,
          "presenti", assembleaState.getPresenti(id),
          "isPresente", assembleaState.getPresenti(id).contains(idUtente),
          "hasDelega", delegheRepository.findDelegaByDeleganteAndIdAssemblea(idUtente, id).isPresent() ,
          "canStart", !assemblea.get().isInCorso() && assemblea.get().getIdProprietario() == idUtente,
          "canStop", assemblea.get().isInCorso() && assemblea.get().getIdProprietario() == idUtente
      ));
      return "assemblee/view";
    }
  }

  @GetMapping("/{id}/presenza")
  public String togglePresenza(@PathVariable("id") Long id) {
    var idUtente = 1L;
    if(assembleaState.getPresenti(id).contains(idUtente)) {
      assembleaState.setAssente(id, idUtente);
    } else {
      assembleaState.setPresente(id, idUtente);
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/inizia")
  public String startAssemblea(@PathVariable("id") Long id) {
    var idUtente = 1L;
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
  public String stopAssemblea(@PathVariable("id") Long id) {
    var idUtente = 1L;
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
