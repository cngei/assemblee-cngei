package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.DelegaEditModel;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;

@Controller
@RequestMapping("/assemblea/{id}/delega")
public class DelegheController {
  private final AssembleeRepository assembleeRepository;
  private final DelegheRepository delegheRepository;
  private final AssembleaState assembleaState;

  public DelegheController(AssembleeRepository assembleeRepository, DelegheRepository delegheRepository, AssembleaState assembleaState) {
    this.assembleeRepository = assembleeRepository;
    this.delegheRepository = delegheRepository;
    this.assembleaState = assembleaState;
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

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      var temp = assemblea.get();
      temp.setPartecipanti(Arrays.stream(temp.getPartecipanti()).filter(x -> x != me).toArray(Long[]::new));
      model.addAttribute("assemblea", temp);
      model.addAttribute("delegaExists", existingDelega.map(Delega::getDelegato).orElse(-1L));
      return "deleghe/create";
    }
  }

  @GetMapping("/annulla")
  public String deleteDelega(@PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(me, id);
    if(existingDelega.isEmpty()) {
      return "redirect:/";
    } else {
      // Se annullo la delega dovro' registrarmi come presente in proprio
      assembleaState.setAssente(id, me);
      delegheRepository.deleteById(existingDelega.get().getId());
      return "redirect:/assemblea/" + id;
    }
  }

  @PostMapping
  public String createDelega(DelegaEditModel delega, @PathVariable("id") Long id, Principal principal) {
    var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
    var assemblea = assembleeRepository.findById(id);
    if (assemblea.isEmpty()) {
      return "redirect:/";
    }
    var newDelega = Delega.builder()
        .idAssemblea(assemblea.get().getId())
        .delegante(me)
        .delegato(Long.parseLong(delega.getTessera()))
        .build();

    // Se la persona che delego e' presente, divento presente per delega
    if(assembleaState.getPresenti(id).contains(Long.parseLong(delega.getTessera()))) {
      assembleaState.setPresente(id, me);
    } else {
      // Altrimenti saro' assente
      assembleaState.setAssente(id, me);
    }
    delegheRepository.save(newDelega);

    return "redirect:/assemblea/" + id;
  }
}
