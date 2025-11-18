package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.DelegaEditModel;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.entities.Socio;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.services.DelegheService;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assemblea/{id}/delega")
public class DelegheController {
  private final AssembleeRepository assembleeRepository;
  private final DelegheRepository delegheRepository;
  private final SocioRepository socioRepository;
  private final DelegheService delegheService;

  public DelegheController(AssembleeRepository assembleeRepository, DelegheRepository delegheRepository,
      SocioRepository socioRepository, DelegheService delegheService) {
    this.assembleeRepository = assembleeRepository;
    this.delegheRepository = delegheRepository;
    this.socioRepository = socioRepository;
    this.delegheService = delegheService;
  }

  @ModelAttribute(name = "delega")
  public DelegaEditModel delega() {
    return new DelegaEditModel();
  }

  @GetMapping
  public String getDelegaView(Model model, @PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleeRepository.findById(id);
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(me, id);
    var allDeleghe = delegheRepository.findAllByIdAssemblea(id);
    var currentDeleghe = allDeleghe.stream().map(Delega::getDelegato).collect(Collectors.toSet());

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      var temp = assemblea.get();
      var nonEligibleForDelega = delegheRepository.findAllByIdAssemblea(id).stream().map(Delega::getDelegato).collect(Collectors.toSet());
      nonEligibleForDelega.add(me);
      
      var partecipanti = Arrays.stream(temp.getPartecipanti())
          .filter(x -> !nonEligibleForDelega.contains(x))
          .map(x -> Map.entry(x, socioRepository.findById(x).map(Socio::getNome).orElse(x.toString())))
          .sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());

      model.addAttribute("assemblea", temp);
      model.addAttribute("partecipanti", partecipanti);
      model.addAttribute("delegaExists",
          existingDelega.flatMap(x -> socioRepository.findById(x.getDelegato())).map(Socio::getNome).orElse(""));
      return "deleghe/create";
    }
  }

  @GetMapping("/annulla")
  public String deleteDelega(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    try {
      delegheService.deleteDelega(id, me);
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/assemblea/" + id;
  }

  @PostMapping
  public String createDelega(DelegaEditModel delega, @PathVariable("id") Long id, Principal principal,
      RedirectAttributes redirectAttributes) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    try {
      delegheService.createDelega(id, me, Long.parseLong(delega.getTessera()));
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/assemblea/" + id + "/delega";
    }

    return "redirect:/assemblea/" + id;
  }
}
