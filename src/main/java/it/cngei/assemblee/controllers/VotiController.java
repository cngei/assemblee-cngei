package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotoEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.entities.Voto;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.repositories.VotiRepository;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.state.VotazioneState;
import it.cngei.assemblee.utils.Utils;
import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

import static com.j256.twofactorauth.TimeBasedOneTimePasswordUtil.generateCurrentNumberString;

@Controller
@RequestMapping("/assemblea/{id}/votazione")
public class VotiController {
  private final AssembleeRepository assembleeRepository;
  private final VotazioneRepository votazioneRepository;
  private final DelegheRepository delegheRepository;
  private final VotiRepository votiRepository;
  private final VotazioneState votazioneState;
  private final AssembleaState assembleaState;

  public VotiController(AssembleeRepository assembleeRepository, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, VotiRepository votiRepository, VotazioneState votazioneState, AssembleaState assembleaState) {
    this.assembleeRepository = assembleeRepository;
    this.votazioneRepository = votazioneRepository;
    this.delegheRepository = delegheRepository;
    this.votiRepository = votiRepository;
    this.votazioneState = votazioneState;
    this.assembleaState = assembleaState;
  }

  @ModelAttribute(name = "votoModel")
  public VotoEditModel votoModel() {
    return new VotoEditModel();
  }

  @GetMapping("/{idVotazione}")
  public String getVotazioneView(
      Model model,
      @PathVariable("id") Long id,
      @PathVariable("idVotazione") Long idVotazione,
      Principal principal,
      VotoEditModel votoModel
  ) {
    var me = Utils.getUserIdFromPrincipal(principal);
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);

    if (assemblea.isEmpty()) {
      throw new NoSuchElementException();
    }
    if (votazione.isEmpty()) {
      return "redirect:/assemblea/" + id;
    }

    if(!assembleaState.getPresenti(id).contains(me)) {
      model.addAllAttributes(Map.of(
          "assemblea", assemblea.get(),
          "votazione", votazione.get(),
          "isPalese", votazione.get().getTipoVotazione() == TipoVotazione.PALESE
      ));
      return "votazioni/preview";
    }

    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(me, id);
    var idProprio = votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? me + "-" + idVotazione : UUID.randomUUID().toString();
    var idDelega = votazione.get().getTipoVotazione() == TipoVotazione.PALESE ? delega.map(Delega::getDelegante).orElse(-1L) + "-" + idVotazione : UUID.randomUUID().toString();

    votoModel.setIdProprio(idProprio);
    votoModel.setIdDelega(idDelega);

    return renderVoto(model, assemblea.get(), votazione.get(), delega.isPresent(), votoModel);
  }

  @SneakyThrows
  @PostMapping("/{idVotazione}")
  public String handleVoto(
      @PathVariable("id") Long id,
      @PathVariable("idVotazione") Long idVotazione,
      @ModelAttribute("votoModel") VotoEditModel votoModel,
      BindingResult bindingResult,
      Principal principal,
      Model model
  ) {
    var me = Long.valueOf(Utils.getKeycloakUserFromPrincipal(principal).getClaim("preferred_username"));
    var assemblea = assembleeRepository.findById(id);
    var votazione = votazioneRepository.findById(idVotazione);
    if (assemblea.isEmpty()) {
      return "redirect:/";
    }
    if (votazione.isEmpty()) {
      return "redirect:/assemblea/" + id;
    }
    var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(me, id);

    if(!assembleaState.getPresenti(id).contains(me)) {
      throw new AccessDeniedException("Devi essere presente per poter votare");
    }
    if(assemblea.get().isRequire2FA()) {
      if(!votoModel.getCode2fa().equals(generateCurrentNumberString(assembleaState.get2faSecret(id, me))))
        throw new AccessDeniedException("La verifica a 2 fattori non corrisponde");
    }
    if (votazioneState.getVotanti(idVotazione).contains(me)) {
      throw new AccessDeniedException("Hai già votato");
    }
    if (votazione.get().isTerminata()) {
      throw new AccessDeniedException("Votazione conclusa");
    }

    Long[] scelteInProprio = null;
    Long[] sceltePerDelega = null;
    try {
      scelteInProprio = parseScelte(votoModel.getInProprio(), votazione.get().getScelte(), votazione.get().getNumeroScelteEffettivo());
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("inProprio", "voto.scelte", "Voto in proprio: " + e.getMessage());
    }
    if (delega.isPresent()) {
      try {
        sceltePerDelega = parseScelte(votoModel.getPerDelega(), votazione.get().getScelte(), votazione.get().getNumeroScelteEffettivo());
      } catch (IllegalArgumentException e) {
        bindingResult.rejectValue("perDelega", "voto.scelte", "Voto per delega: " + e.getMessage());
      }
    }
    if (bindingResult.hasErrors()) {
      return renderVoto(model, assemblea.get(), votazione.get(), delega.isPresent(), votoModel);
    }

    var inProprio = Voto.builder()
        .id(votoModel.getIdProprio())
        .idVotazione(idVotazione)
        .scelte(scelteInProprio)
        .build();
    votiRepository.save(inProprio);
    votazioneState.setVotante(idVotazione, me);

    if (delega.isPresent()) {
      var perDelega = Voto.builder()
          .id(votoModel.getIdDelega())
          .idVotazione(idVotazione)
          .scelte(sceltePerDelega)
          .perDelega(true)
          .build();
      votiRepository.save(perDelega);
      votazioneState.setVotante(idVotazione, delega.get().getDelegante());
    }
    if(votazione.get().getTipoVotazione() == TipoVotazione.PALESE) {
      return "redirect:/assemblea/" + id + "/votazione/" + idVotazione + "/risultati";
    } else {
      return "redirect:/assemblea/" + id;
    }
  }

  private String renderVoto(Model model, Assemblea assemblea, Votazione votazione, boolean hasDelega, VotoEditModel votoModel) {
    model.addAllAttributes(Map.of(
        "assemblea", assemblea,
        "votazione", votazione,
        "hasDelega", hasDelega,
        "isPalese", votazione.getTipoVotazione() == TipoVotazione.PALESE,
        "votoModel", votoModel
    ));
    model.addAttribute("idProprio", votoModel.getIdProprio());
    model.addAttribute("idDelega", hasDelega ? votoModel.getIdDelega() : -1L);
    return "votazioni/view";
  }

  private Long[] parseScelte(List<String> scelte, String[] opzioni, long maxScelte) {
    var selezionate = scelte == null ? List.<String>of() : scelte.stream().filter(Objects::nonNull).toList();
    if (selezionate.isEmpty()) {
      return new Long[]{(long) (opzioni.length - 1)};
    }
    if (selezionate.size() > maxScelte) {
      throw new IllegalArgumentException("Puoi selezionare al massimo " + maxScelte + (maxScelte == 1 ? " opzione." : " opzioni."));
    }

    var opzioniDisponibili = Arrays.asList(opzioni);
    Set<Long> indici = new LinkedHashSet<>();
    for (var scelta : selezionate) {
      var indice = opzioniDisponibili.indexOf(scelta);
      if (indice < 0) {
        throw new IllegalArgumentException("Una delle opzioni selezionate non è valida.");
      }
      if (!indici.add((long) indice)) {
        throw new IllegalArgumentException("Ogni opzione può essere selezionata una sola volta.");
      }
    }
    return indici.toArray(Long[]::new);
  }
}
