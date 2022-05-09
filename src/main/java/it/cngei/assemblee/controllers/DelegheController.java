package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.DelegaEditModel;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Controller
@RequestMapping("/assemblea/{id}/delega")
public class DelegheController {
  private final AssembleeRepository assembleeRepository;
  private final DelegheRepository delegheRepository;

  public DelegheController(AssembleeRepository assembleeRepository, DelegheRepository delegheRepository) {
    this.assembleeRepository = assembleeRepository;
    this.delegheRepository = delegheRepository;
  }

  @ModelAttribute(name = "delega")
  public DelegaEditModel delega() {
    return new DelegaEditModel();
  }

  @GetMapping
  public String getDelegaView(Model model, @PathVariable("id") Long id) {
    var assemblea = assembleeRepository.findById(id);
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(1L, id);

    if (assemblea.isEmpty()) {
      return "redirect:/";
    } else {
      var temp = assemblea.get();
      temp.setPartecipanti(Arrays.stream(temp.getPartecipanti()).filter(x -> x != 1L).toArray(Long[]::new));
      model.addAttribute("assemblea", temp);
      model.addAttribute("delegaExists", existingDelega.map(Delega::getDelegato).orElse(-1L));
      return "deleghe/create";
    }
  }

  @GetMapping("/annulla")
  public String deleteDelega(@PathVariable("id") Long id) {
    var existingDelega = delegheRepository.findDelegaByDeleganteAndIdAssemblea(1L, id);
    if(existingDelega.isEmpty()) {
      return "redirect:/";
    } else {
      delegheRepository.deleteById(existingDelega.get().getId());
      return "redirect:/assemblea/" + id;
    }
  }

  @PostMapping
  public String createDelega(DelegaEditModel delega, @PathVariable("id") Long id) {
    var assemblea = assembleeRepository.findById(id);
    if (assemblea.isEmpty()) {
      return "redirect:/";
    }
    var newDelega = Delega.builder()
        .idAssemblea(assemblea.get().getId())
        .delegante(1L)
        .delegato(Long.parseLong(delega.getTessera()))
        .build();
    delegheRepository.save(newDelega);

    return "redirect:/assemblea/" + id;
  }
}
