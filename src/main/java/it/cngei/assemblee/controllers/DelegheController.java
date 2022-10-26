package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.DelegaEditModel;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.entities.Socio;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.services.AssembleaService;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
  private final AssembleaState assembleaState;
  private final AssembleaService assembleaService;

  public DelegheController(AssembleeRepository assembleeRepository, DelegheRepository delegheRepository, SocioRepository socioRepository, AssembleaState assembleaState, AssembleaService assembleaService) {
    this.assembleeRepository = assembleeRepository;
    this.delegheRepository = delegheRepository;
    this.socioRepository = socioRepository;
    this.assembleaState = assembleaState;
    this.assembleaService = assembleaService;
  }

  @ModelAttribute(name = "delega")
  public DelegaEditModel delega() {
    return new DelegaEditModel();
  }

  @GetMapping
  public String getDelegaView(Model model, @PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var assemblea = assembleeRepository.findById(id);
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(me, id);
    var allDeleghe = delegheRepository.findAllByIdAssemblea(id);
    var currentDeleghe = allDeleghe.stream().map(Delega::getDelegato).collect(Collectors.toSet());
    currentDeleghe.addAll(allDeleghe.stream().map(Delega::getDelegante).collect(Collectors.toSet()));

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      var temp = assemblea.get();
      temp.setPartecipanti(Arrays.stream(temp.getPartecipanti()).filter(x -> x != me && !currentDeleghe.contains(x)).toArray(Long[]::new));
      var partecipanti = Arrays.stream(temp.getPartecipanti()).map(x -> Map.entry(x, socioRepository.findById(x).map(Socio::getNome).orElse(x.toString()))).sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
      model.addAttribute("assemblea", temp);
      model.addAttribute("partecipanti", partecipanti);
      model.addAttribute("delegaExists", existingDelega.flatMap(x -> socioRepository.findById(x.getDelegato())).map(Socio::getNome).orElse(""));
      return "deleghe/create";
    }
  }

  @GetMapping("/annulla")
  @CacheEvict(value = {"deleghe"}, key = "#id")
  public String deleteDelega(@PathVariable("id") Long id, Principal principal) {
    var assemblea = assembleaService.getAssemblea(id);
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(me, id);
    if(existingDelega.isEmpty()) {
      return "redirect:/";
    } else {
      // Se annullo la delega dovro' registrarmi come presente in proprio
      assembleaState.setAssente(id, me, assemblea.isRequire2FA());
      delegheRepository.deleteById(existingDelega.get().getId());
      return "redirect:/assemblea/" + id;
    }
  }

  @PostMapping
  @CacheEvict(value = {"deleghe"}, key = "#id")
  public String createDelega(DelegaEditModel delega, @PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var assemblea = assembleaService.getAssemblea(id);

    if(delegheRepository.findDelegaByDeleganteAndIdAssemblea(me, id).isPresent()) {
      throw new IllegalStateException("Delega già presente");
    }

    var newDelega = Delega.builder()
        .idAssemblea(assemblea.getId())
        .delegante(me)
        .delegato(Long.parseLong(delega.getTessera()))
        .build();

    // Se la persona che delego e' presente, divento presente per delega
    if(assembleaState.getPresenti(id).contains(Long.parseLong(delega.getTessera()))) {
      assembleaState.setPresente(id, me, assemblea.isRequire2FA());
    } else {
      // Altrimenti saro' assente
      assembleaState.setAssente(id, me, assemblea.isRequire2FA());
    }
    delegheRepository.save(newDelega);

    return "redirect:/assemblea/" + id;
  }
}
