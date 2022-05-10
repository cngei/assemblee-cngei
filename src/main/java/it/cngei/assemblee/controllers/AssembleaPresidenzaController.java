package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.NominaPresidenteEditModel;
import it.cngei.assemblee.dtos.OdgEditModel;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Controller
@RequestMapping("/assemblea")
public class AssembleaPresidenzaController {
  private final AssembleeRepository assembleeRepository;

  public AssembleaPresidenzaController(AssembleeRepository assembleeRepository) {
    this.assembleeRepository = assembleeRepository;
  }

  @ModelAttribute(name = "odgModel")
  public OdgEditModel odgModel() {
    return new OdgEditModel();
  }

  @ModelAttribute(name = "presidenteModel")
  public NominaPresidenteEditModel presidenteModel() {
    return new NominaPresidenteEditModel();
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
}
