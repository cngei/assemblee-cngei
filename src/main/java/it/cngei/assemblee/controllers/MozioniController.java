package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.MozioneEditModel;
import it.cngei.assemblee.dtos.MozioneViewModel;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.services.AssembleaService;
import it.cngei.assemblee.services.MozioniService;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assemblea/{id}/mozioni")
public class MozioniController {
  private final MozioniService mozioniService;
  private final AssembleaService assembleaService;
  private final SocioRepository socioRepository;

  public MozioniController(MozioniService mozioniService, AssembleaService assembleaService, SocioRepository socioRepository) {
    this.mozioniService = mozioniService;
    this.assembleaService = assembleaService;
    this.socioRepository = socioRepository;
  }

  @ModelAttribute(name = "mozione")
  public MozioneEditModel mozione() {
    return new MozioneEditModel();
  }

  @GetMapping
  public String getMozioniView(@PathVariable("id") Long idAssemblea, Model model, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    var assemblea = assembleaService.getAssemblea(idAssemblea);
    var mozioni = mozioniService.getByAssemblea(idAssemblea);

    model.addAttribute("idAssemblea", idAssemblea);
    model.addAttribute("mozioni", mozioni.stream().map(x -> new MozioneViewModel(x, idUtente, id -> socioRepository.findById(id).map(s -> s.getNome() + " - " + s.getSezione()).orElse(id.toString()))).collect(Collectors.toList()));
    model.addAttribute("me", idUtente);
    model.addAttribute("readOnly", !assemblea.isMozioniOpen());
    model.addAttribute("isAdmin", Utils.isAdmin(assemblea, idUtente));
    return "mozioni/list";
  }

  @GetMapping("/crea")
  public String creaMozioneView(@PathVariable("id") Long idAssemblea, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsDelegato(idAssemblea, idUtente);
    return "mozioni/create";
  }

  @PostMapping("/crea")
  public String creaMozione(@PathVariable("id") Long idAssemblea, MozioneEditModel mozioneEditModel, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsDelegato(idAssemblea, idUtente);
    mozioniService.create(mozioneEditModel, idAssemblea, idUtente);
    return "redirect:/assemblea/" + idAssemblea;
  }

  @GetMapping("/{idMozione}")
  public String firmaMozione(@PathVariable("id") Long idAssemblea, @PathVariable("idMozione") Long idMozione, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsDelegato(idAssemblea, idUtente);
    mozioniService.firma(idMozione, idUtente);
    return "redirect:/assemblea/" + idAssemblea + "/mozioni";
  }

  @GetMapping("/stop")
  public String stopMozioni(@PathVariable("id") Long idAssemblea, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsAdmin(idAssemblea, idUtente);
    assembleaService.stopMozioni(idAssemblea);
    return "redirect:/assemblea/" + idAssemblea;
  }

  @GetMapping("/start")
  public String startMozioni(@PathVariable("id") Long idAssemblea, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsAdmin(idAssemblea, idUtente);
    assembleaService.startMozioni(idAssemblea);
    return "redirect:/assemblea/" + idAssemblea;
  }

  @GetMapping("/{idMozione}/converti")
  public String convertiMozione(@PathVariable("id") Long idAssemblea, @PathVariable("idMozione") Long idMozione, Principal principal) {
    Long idUtente = Utils.getUserIdFromPrincipal(principal);
    assembleaService.checkIsAdmin(idAssemblea, idUtente);
    mozioniService.converti(idMozione);
    return "redirect:/assemblea/" + idAssemblea;
  }
}
