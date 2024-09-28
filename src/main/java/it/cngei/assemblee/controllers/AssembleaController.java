package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.AssembleaEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.repositories.AssembleeRepository;
import it.cngei.assemblee.repositories.DelegheRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import it.cngei.assemblee.services.AssembleaService;
import it.cngei.assemblee.services.DelegheService;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.state.VotazioneState;
import it.cngei.assemblee.utils.Utils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.j256.twofactorauth.TimeBasedOneTimePasswordUtil.qrImageUrl;

@Controller
@RequestMapping("/assemblea")
public class AssembleaController {
    private final AssembleeRepository assembleeRepository;
    private final AssembleaService assembleaService;
    private final VotazioneRepository votazioneRepository;
    private final DelegheRepository delegheRepository;
    private final SocioRepository socioRepository;
    private final AssembleaState assembleaState;
    private final VotazioneState votazioneState;
    private final DelegheService delegheService;
    private final CacheManager cacheManager;

    public AssembleaController(AssembleeRepository assembleeRepository, AssembleaService assembleaService, VotazioneRepository votazioneRepository, DelegheRepository delegheRepository, SocioRepository socioRepository, AssembleaState assembleaState, VotazioneState votazioneState, DelegheService delegheService, CacheManager cacheManager) {
        this.assembleeRepository = assembleeRepository;
        this.assembleaService = assembleaService;
        this.votazioneRepository = votazioneRepository;
        this.delegheRepository = delegheRepository;
        this.socioRepository = socioRepository;
        this.assembleaState = assembleaState;
        this.votazioneState = votazioneState;
        this.delegheService = delegheService;
        this.cacheManager = cacheManager;
    }

    @ModelAttribute(name = "assembleaModel")
    public AssembleaEditModel assembleaModel() {
        return new AssembleaEditModel();
    }

    @GetMapping("/{id}")
    public String getAssemblea(Model model, @PathVariable("id") Long id, Principal principal) {
        var assemblea = assembleaService.getAssemblea(id);
        var idUtente = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        var isDelegato = Arrays.asList(assemblea.getPartecipanti()).contains(idUtente);
        var presenti = assembleaState.getPresenti(id);
        var isAdmin = idUtente == assemblea.getIdProprietario() || (assemblea.getIdPresidente() != null && idUtente == assemblea.getIdPresidente());

        var votazioni = votazioneRepository.findAllByIdAssemblea(id).stream().filter(x -> isAdmin || (x.isAperta() || x.isTerminata())).collect(Collectors.toList());

        model.addAllAttributes(Map.of(
                "assemblea", assemblea,
                "votazioni", votazioni,
                "presenti", presenti,
                "isPresente", presenti.contains(idUtente),
                "hasDelega", delegheRepository.findDelegaByDeleganteAndIdAssemblea(idUtente, id).isPresent(),
                "canStart", !assemblea.isInCorso() && assemblea.getIdProprietario().equals(idUtente),
                "canStop", assemblea.isInCorso() && assemblea.getIdProprietario().equals(idUtente),
                "votazioneState", votazioneState,
                "tessera", idUtente,
                "isProprietario", isAdmin
        ));

        model.addAttribute("presentiTotali", assembleaService.getPresentiTotali(id));
        model.addAttribute("canSetPresenza", isDelegato && assemblea.isInCorso() && !presenti.contains(idUtente) && votazioni.stream().allMatch(x -> !x.isAperta() || x.isTerminata()) && !assemblea.isInPresenza());
        model.addAttribute("canRemovePresenza", isDelegato && assemblea.isInCorso() && presenti.contains(idUtente) && votazioni.stream().allMatch(x -> !x.isAperta() || x.isTerminata()) && !assemblea.isInPresenza());
        model.addAttribute("canDelega", isDelegato && !assemblea.isNazionale());
        model.addAttribute("isDelegato", isDelegato);
        model.addAttribute("isCovepo", Utils.isCovepo(assemblea, idUtente));
        model.addAttribute("ownsDelega", delegheRepository.findDelegaByDelegatoAndIdAssemblea(idUtente, id).isPresent());
        return "assemblee/view";
    }

    @GetMapping("/{id}/presenza")
    public String togglePresenza(@PathVariable("id") Long id, Principal principal) {
        var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        var delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(me, id);
        var assemblea = assembleaService.getAssemblea(id);
        var votazioni = votazioneRepository.findAllByIdAssemblea(id);
        var presenti = assembleaState.getPresenti(id);
        if (presenti.contains(me) && votazioni.stream().anyMatch(x -> x.isAperta() && !x.isTerminata())) {
            throw new IllegalStateException("Non puoi segnarti come assente durante una votazione");
        }
        if (presenti.contains(me)) {
            assembleaState.setAssente(id, me, assemblea.isRequire2FA());
            delega.ifPresent(value -> assembleaState.setAssente(id, value.getDelegante(), assemblea.isRequire2FA()));
        } else {
            assembleaState.setPresente(id, me, assemblea.isRequire2FA());
            delega.ifPresent(value -> assembleaState.setPresente(id, value.getDelegante(), assemblea.isRequire2FA()));
            if (assemblea.isRequire2FA()) {
                return "redirect:/assemblea/" + id + "/2fa";
            }
        }
        return "redirect:/assemblea/" + id;
    }

    @GetMapping("/{id}/2fa")
    public String get2fa(@PathVariable("id") Long id, Principal principal, Model model) {
        var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        model.addAttribute("key", qrImageUrl("Assemblea - " + me, assembleaState.get2faSecret(id, me)));
        model.addAttribute("assembleaId", id);
        return "assemblee/2fa";
    }

    @GetMapping("/{id}/presenti")
    public String getPresenti(@PathVariable("id") Long id, Model model, Principal principal) {
        var assemblea = assembleaService.getAssemblea(id);
        var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        var deleghe = delegheService.getDeleghe(id);
        model.addAttribute("partecipanti", assembleaService.getPartecipanti(id));
        model.addAttribute("presenti", assembleaState.getPresenti(id));
        model.addAttribute("assembleaId", id);
        model.addAttribute("deleghe", deleghe);
        model.addAttribute("isCovepo", Utils.isCovepo(assemblea, me));
        return "assemblee/presenti";
    }

    @PostMapping("/{id}/caccia/{idUtente}")
    public ResponseEntity kickPartecipante(@PathVariable("id") Long id, @PathVariable("idUtente") Long idUtente, Principal principal) {
        Long me = Utils.getUserIdFromPrincipal(principal);
        assembleaService.checkIsAdmin(id, me);

        var assemblea = assembleaService.getAssemblea(id);
        Optional<Delega> delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(idUtente, id);

        assembleaState.setAssente(id, idUtente, assemblea.isRequire2FA());
        delega.ifPresent(x -> assembleaState.setAssente(id, x.getDelegante(), assemblea.isRequire2FA()));
        return ResponseEntity.ok("<button hx-post='/assemblea/" + id + "/presente/" + idUtente + " ' type='button' class='btn btn-outline btn-sm mb-1' hx-swap='outerHTML'>Segna presente</button>");
    }

    @GetMapping("/{id}/caccia")
    public String kickEverybody(@PathVariable("id") Long id, Principal principal) {
        var assemblea = assembleaService.getAssemblea(id);
        var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        assembleaService.checkIsAdmin(id, me);
        assembleaState.clearPresenti(id, assemblea.isRequire2FA());
        return "redirect:/assemblea/" + id + "/presenti";
    }

    @PostMapping("/{id}/presente/{idUtente}")
    public ResponseEntity segnaPresente(@PathVariable("id") Long id, @PathVariable("idUtente") Long idUtente, Principal principal) {
        Long me = Utils.getUserIdFromPrincipal(principal);
        assembleaService.checkIsAdminOrCovepo(id, me);
        var assemblea = assembleaService.getAssemblea(id);

        Optional<Delega> delega = delegheRepository.findDelegaByDelegatoAndIdAssemblea(idUtente, id);

        if (delega.isPresent()) {
            assembleaState.setPresente(id, new Long[]{idUtente, delega.get().getDelegante()});
        } else {
            assembleaState.setPresente(id, idUtente, assemblea.isRequire2FA());
        }

        return ResponseEntity.ok("<button hx-post='/assemblea/" + id + "/caccia/" + idUtente + " ' type='button' class='btn btn-outline btn-sm mb-1' hx-swap='outerHTML'>Segna assente</button>");
//        return "redirect:/assemblea/" + id + "/presenti";
    }

    @GetMapping("crea")
    public String getCreateAssemblea(Model model, Principal principal) {
        var me = Utils.getKeycloakUserFromPrincipal(principal);
        List<String> groups = (List<String>) me.getOtherClaims().get("groups");
        var sezione = groups.stream().filter(x -> x.matches("/[\\w\\s']+")).findFirst().map(x -> x.substring(1));
        if (sezione.isPresent()) {
            model.addAttribute("nomeSezione", sezione.get());
            var soci = socioRepository.findBySezione(sezione.get());
            model.addAttribute("soci", soci.stream().map(x -> String.valueOf(x.getId())).collect(Collectors.joining("\n")));
        }
        return "assemblee/create";
    }

    @PostMapping("/crea")
    public String createAssemblea(AssembleaEditModel assembleaModel, Principal principal) {
        var me = Long.parseLong(Utils.getKeycloakUserFromPrincipal(principal).getPreferredUsername());
        var newAssemblea = Assemblea.builder()
                .idProprietario(me)
                .nome(assembleaModel.getNome())
                .descrizione(assembleaModel.getDescrizione())
                .partecipanti(parsePartecipanti(assembleaModel.getPartecipanti()))
                .totaleDelegati(assembleaModel.getTotalePartecipanti())
                .convocazione(LocalDateTime.parse(assembleaModel.getDateTime()))
                .stepOdg(0L)
                .odg(Arrays.stream(assembleaModel.getOdg().split("\n")).filter(x -> !x.isBlank()).toArray(String[]::new))
                .require2FA(assembleaModel.isRequire2fa())
                .isNazionale(assembleaModel.isNazionale())
                .isInPresenza(assembleaModel.isInPresenza())
                .build();

        assembleeRepository.save(newAssemblea);
        return "redirect:/";
    }

    @PostMapping("/{id}/aggiungiPartecipante")
    public String aggiungiPartecipante(@PathVariable("id") Long id, @RequestParam String tessera, Principal principal) {
        Long user = Utils.getUserIdFromPrincipal(principal);
        assembleaService.checkIsAdmin(id, user);
        assembleaService.addDelegato(id, Long.parseLong(tessera));
        return "redirect:/assemblea/" + id + "/presenti";
    }

    @GetMapping("/{id}/rimuoviPartecipante/{tessera}")
    public String rimuoviPartecipante(@PathVariable("id") Long id, @PathVariable Long tessera, Principal principal) {
        Long user = Utils.getUserIdFromPrincipal(principal);
        assembleaService.checkIsAdmin(id, user);
        assembleaState.setAssente(id, tessera, false);
        Optional<Delega> delegaByDelegatoAndIdAssemblea = delegheRepository.findDelegaByDelegatoAndIdAssemblea(tessera, id);
        delegaByDelegatoAndIdAssemblea.ifPresent(x -> {
            assembleaState.setAssente(id, x.getDelegante(), false);
            delegheRepository.delete(x);
        });
        assembleaService.removeDelegato(id, tessera);
        return "redirect:/assemblea/" + id + "/presenti";
    }

    @PostMapping("/{id}/covepoDelega")
    @CacheEvict(value = {"deleghe"}, key = "#id")
    public String covepoDelega(@PathVariable("id") Long id, @RequestParam String delegante, @RequestParam String delegato, Principal principal) {
        Long user = Utils.getUserIdFromPrincipal(principal);
        assembleaService.checkIsAdmin(id, user);

        if (delegheRepository.findDelegaByDeleganteAndIdAssemblea(Long.parseLong(delegante), id).isPresent()) {
            throw new IllegalStateException("Delega già presente");
        }

        var newDelega = Delega.builder()
                .idAssemblea(id)
                .delegante(Long.parseLong(delegante))
                .delegato(Long.parseLong(delegato))
                .build();

        // Se la persona che delego e' presente, divento presente per delega
        if (assembleaState.getPresenti(id).contains(Long.parseLong(delegato))) {
            assembleaState.setPresente(id, Long.parseLong(delegante), false);
        } else {
            // Altrimenti saro' assente
            assembleaState.setAssente(id, Long.parseLong(delegante), false);
        }
        delegheRepository.save(newDelega);

        return "redirect:/assemblea/" + id + "/presenti";
    }

    private Long[] parsePartecipanti(String original) {
        return Arrays.stream(original.split("\n"))
                .map(String::trim)
                .filter(x -> Pattern.matches("\\d+", x))
                .map(Long::parseLong)
                .collect(Collectors.toSet())
                .toArray(Long[]::new);
    }

    @GetMapping("/cache")
    public ResponseEntity<String> evictCaches() {
        cacheManager.getCacheNames().stream().forEach(x -> cacheManager.getCache(x).clear());
        return ResponseEntity.ok("Cache cleared");
    }
}
