package it.cngei.assemblee.controllers;

import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.utils.Utils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class HomeController {
  private final AssembleeRepository assembleeRepository;

  public HomeController(AssembleeRepository assembleeRepository) {
    this.assembleeRepository = assembleeRepository;
  }

  @GetMapping
  public String getHome(Model model, Principal principal) {
    var token = Utils.getKeycloakUserFromPrincipal(principal);
    Map<Boolean, List<Assemblea>> assemblee = assembleeRepository
        .findVisible(Long.parseLong(token.getClaim("preferred_username"))).stream()
        .collect(Collectors.groupingBy(x -> x.getFine() == null));
    model.addAttribute("assemblee", assemblee.get(Boolean.TRUE));
    model.addAttribute("oldAssemblee", assemblee.get(Boolean.FALSE));
    return "assemblee/list";
  }

  @GetMapping("/logout")
  public void logout(HttpServletRequest request) throws ServletException {
    request.logout();
  }

  @GetMapping("/post-logout")
  public String postLogout() {
    return "redirect:/";
  }

}
