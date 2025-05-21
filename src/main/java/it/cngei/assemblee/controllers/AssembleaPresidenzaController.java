package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.MessageModel;
import it.cngei.assemblee.dtos.NominaCovepoEditModel;
import it.cngei.assemblee.dtos.NominaPresidenteEditModel;
import it.cngei.assemblee.dtos.OdgEditModel;
import it.cngei.assemblee.enums.TipoMessaggio;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.services.AssembleaService;
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
  private final SocioRepository socioRepository;
  private final AssembleaService assembleaService;
  private final MessageController messageController;

  public AssembleaPresidenzaController(AssembleeRepository assembleeRepository, SocioRepository socioRepository, AssembleaService assembleaService, MessageController messageController) {
    this.assembleeRepository = assembleeRepository;
    this.socioRepository = socioRepository;
    this.assembleaService = assembleaService;
    this.messageController = messageController;
  }

  @ModelAttribute(name = "odgModel")
  public OdgEditModel odgModel() {
    return new OdgEditModel();
  }

  @ModelAttribute(name = "presidenteModel")
  public NominaPresidenteEditModel presidenteModel() {
    return new NominaPresidenteEditModel();
  }

  @ModelAttribute(name = "covepoModel")
  public NominaCovepoEditModel covepoModel() {
    return new NominaCovepoEditModel();
  }

  @GetMapping("/{id}/nomina-presidente")
  public String getNominaPresidente(@PathVariable("id") Long id, Model model, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    var partecipanti = socioRepository.findAllById(Arrays.asList(assemblea.getPartecipanti()));

    model.addAttribute("partecipanti", partecipanti);
    model.addAttribute("assemblea", assemblea);
    return "assemblee/nominaPresidente";
  }

  @PostMapping("/{id}/nomina-presidente")
  public String nominaPresidente(@PathVariable("id") Long id, NominaPresidenteEditModel presidenteEditModel, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    // FIXME: controllare che il presidente sia tra i partecipanti
    assemblea.setIdPresidente(presidenteEditModel.getTessera());
    assembleeRepository.save(assemblea);
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/nomina-covepo")
  public String getNominaCovepo(@PathVariable("id") Long id, Model model, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    var partecipanti = socioRepository.findAllById(Arrays.asList(assemblea.getPartecipanti()));

    model.addAttribute("partecipanti", partecipanti);
    model.addAttribute("assemblea", assemblea);
    return "assemblee/nominaCovepo";
  }

  @PostMapping("/{id}/nomina-covepo")
  public String nominaCovepo(@PathVariable("id") Long id, NominaCovepoEditModel nominaCovepoEditModel, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    // FIXME: controllare che i membri della CoVePo siano tra i partecipanti
    assemblea.setCovepo(nominaCovepoEditModel.getTessere());
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
    var assemblea = assembleaService.getAssemblea(id);

    assemblea.setOdg(Arrays.stream(odgEditModel.getOdg().split("\n"))
        .filter(x -> !x.isBlank())
        .map(String::trim)
        .toArray(String[]::new)
    );
    assembleeRepository.save(assemblea);
    messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.STEP_ODG).build());

    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/avanza-odg")
  public String nextOdg(@PathVariable("id") Long id) {
    assembleaService.nextOdg(id);
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/indietro-odg")
  public String previousOdg(@PathVariable("id") Long id) {
    assembleaService.previousOdg(id);

    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/inizia")
  public String startAssemblea(@PathVariable("id") Long id, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var maybeAssemblea = assembleeRepository.findById(id);
    if(maybeAssemblea.isEmpty()) {
      return "redirect:/";
    }
    var assemblea = maybeAssemblea.get();
    if (!assemblea.isInCorso() && assemblea.getFine() == null && (assemblea.getIdProprietario() == idUtente || assemblea.getIdPresidente() == idUtente)) {
      assemblea.setInCorso(true);
      assembleeRepository.save(assemblea);
      messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.INIZIO).build());
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/termina")
  public String stopAssemblea(@PathVariable("id") Long id, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    if (assemblea.isInCorso()) {
      assemblea.setInCorso(false);
      assemblea.setFine(LocalDateTime.now());
      assembleeRepository.save(assemblea);
      messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.FINE).build());
    }
    return "redirect:/assemblea/" + id;
  }

  @GetMapping("/{id}/toggle-mozioni")
  public String toggleMozioni(@PathVariable("id") Long id, Principal principal) {
    var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleaService.getAssemblea(id);
    assembleaService.checkIsAdmin(id, idUtente);

    if (assemblea.getFine() == null) {
      assemblea.setMozioniOpen(!assemblea.isMozioniOpen());
      assembleeRepository.save(assemblea);
      messageController.send(MessageModel.builder().idAssemblea(id).tipoMessaggio(TipoMessaggio.INIZIO).build());
    }

    return "redirect:/assemblea/" + id;
  }
}
