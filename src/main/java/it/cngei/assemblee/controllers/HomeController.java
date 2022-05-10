package it.cngei.assemblee.controllers;

import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.utils.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

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
    model.addAttribute("assemblee", assembleeRepository.findVisible(Long.parseLong(token.getPreferredUsername())));
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
