package it.cngei.assemblee.controllers;

import it.cngei.assemblee.dtos.VotoEditModel;
import it.cngei.assemblee.entities.Assemblea;
import it.cngei.assemblee.entities.Delega;
import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.entities.Voto;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.*;
import it.cngei.assemblee.services.AssembleaService;
import it.cngei.assemblee.state.AssembleaState;
import it.cngei.assemblee.state.VotazioneState;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class VotiControllerTest {
  private static final String URL = "/assemblea/1/votazione/2";
  private static final String CREATE_URL = "/assemblea/1/votazione/crea";
  private final AssembleeRepository assemblee = mock(AssembleeRepository.class);
  private final VotazioneRepository votazioni = mock(VotazioneRepository.class);
  private final DelegheRepository deleghe = mock(DelegheRepository.class);
  private final VotiRepository voti = mock(VotiRepository.class);
  private final VotazioneState votazioneState = mock(VotazioneState.class);
  private final AssembleaState assembleaState = mock(AssembleaState.class);
  private final AssembleaService assembleaService = mock(AssembleaService.class);
  private final MessageController messages = mock(MessageController.class);
  private final Assemblea assemblea = Assemblea.builder().id(1L).nome("Assemblea di prova").build();
  private final Votazione votazione = Votazione.builder().id(2L).idAssemblea(1L)
      .quesito("Votazione di prova").tipoVotazione(TipoVotazione.SEGRETO).numeroScelte(2L)
      .scelte(new String[]{"Prima", "Seconda", "Terza", "Astenuto"}).build();
  private MockMvc mvc;
  private OAuth2AuthenticationToken principal;

  @BeforeEach
  void setup() {
    var authority = new SimpleGrantedAuthority("ROLE_USER");
    var token = new OidcIdToken("test", Instant.now(), Instant.now().plusSeconds(600),
        Map.of("sub", "42", "preferred_username", "42"));
    principal = new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(authority), token), List.of(authority), "keycloak");
    when(assemblee.findById(1L)).thenReturn(Optional.of(assemblea));
    when(votazioni.findById(2L)).thenReturn(Optional.of(votazione));
    when(deleghe.findDelegaByDelegatoAndIdAssemblea(42L, 1L)).thenReturn(Optional.empty());
    when(assembleaState.getPresenti(1L)).thenReturn(Set.of(42L));
    when(votazioneState.getVotanti(2L)).thenReturn(Set.of());
    when(assembleaService.getAssemblea(1L)).thenReturn(assemblea);

    var resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setCharacterEncoding("UTF-8");
    var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.addDialect(new LayoutDialect());
    var views = new ThymeleafViewResolver();
    views.setTemplateEngine(engine);
    views.setCharacterEncoding("UTF-8");

    mvc = standaloneSetup(
        new VotiController(assemblee, votazioni, deleghe, voti, votazioneState, assembleaState),
        new VotazioniController(assemblee, votazioni, voti, mock(SocioRepository.class), assembleaState, messages, assembleaService)
    ).setViewResolvers(views).build();
  }

  private MockHttpServletRequestBuilder vote() {
    return post(URL).principal(principal).param("idProprio", "own-vote").param("idDelega", "delegated-vote");
  }

  private void hasDelega() {
    when(deleghe.findDelegaByDelegatoAndIdAssemblea(42L, 1L))
        .thenReturn(Optional.of(Delega.builder().delegante(99L).delegato(42L).idAssemblea(1L).build()));
  }

  private void assertNoVoteRecorded() {
    verifyNoInteractions(voti);
    verify(votazioneState, never()).setVotante(any(), any());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {-1, 0, 1})
  void singleChoiceWorksWithLegacyInvalidLimits(Long limit) throws Exception {
    votazione.setNumeroScelte(limit);
    mvc.perform(vote().param("inProprio", "Seconda"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Voto.class);
    verify(voti).save(saved.capture());
    assertThat(saved.getValue().getScelte()).containsExactly(1L);
    verify(votazioneState).setVotante(2L, 42L);
  }

  @Test
  void tooManyChoicesAreRejectedAndPreservedInTheForm() throws Exception {
    var result = mvc.perform(vote().param("inProprio", "Prima", "Seconda", "Terza", "Astenuto"))
        .andExpect(status().isOk()).andExpect(view().name("votazioni/view"))
        .andExpect(model().attributeHasFieldErrors("votoModel", "inProprio"))
        .andExpect(model().attribute("idProprio", "own-vote"))
        .andReturn();
    var submitted = (VotoEditModel) result.getModelAndView().getModel().get("votoModel");
    assertThat(submitted.getInProprio()).containsExactly("Prima", "Seconda", "Terza", "Astenuto");
    var html = result.getResponse().getContentAsString();
    assertThat(html).contains("Puoi selezionare al massimo 2 opzioni.", "data-max-selected=\"2\"", "/js/voto.js");
    assertThat(html.split("checked=\"checked\"", -1)).hasSize(5);
    assertNoVoteRecorded();
  }

  @Test
  void invalidDelegatedVoteDoesNotRecordEitherVote() throws Exception {
    hasDelega();
    mvc.perform(vote().param("inProprio", "Prima").param("perDelega", "Prima", "Seconda", "Astenuto"))
        .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("votoModel", "perDelega"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Voto per delega:")));
    assertNoVoteRecorded();
  }

  @Test
  void bothInvalidGroupsAreReportedWithoutSaving() throws Exception {
    hasDelega();
    mvc.perform(vote().param("inProprio", "Prima", "Seconda", "Terza")
        .param("perDelega", "Prima", "Seconda", "Terza"))
        .andExpect(model().attributeHasFieldErrors("votoModel", "inProprio", "perDelega"));
    assertNoVoteRecorded();
  }

  @Test
  void choicesAtTheLimitAreRecordedForBothGroups() throws Exception {
    hasDelega();
    mvc.perform(vote().param("inProprio", "Prima", "Terza").param("perDelega", "Seconda", "Astenuto"))
        .andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Voto.class);
    verify(voti, times(2)).save(saved.capture());
    assertThat(saved.getAllValues().get(0).getScelte()).containsExactly(0L, 2L);
    assertThat(saved.getAllValues().get(1).getScelte()).containsExactly(1L, 3L);
    assertThat(saved.getAllValues().get(1).isPerDelega()).isTrue();
    verify(votazioneState).setVotante(2L, 42L);
    verify(votazioneState).setVotante(2L, 99L);
  }

  @Test
  void rejectedVoteCanBeCorrectedAndResubmitted() throws Exception {
    mvc.perform(vote().param("inProprio", "Prima", "Seconda", "Terza"))
        .andExpect(model().attributeHasFieldErrors("votoModel", "inProprio"));
    assertNoVoteRecorded();
    mvc.perform(vote().param("inProprio", "Seconda", "Terza"))
        .andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Voto.class);
    verify(voti).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo("own-vote");
    assertThat(saved.getValue().getScelte()).containsExactly(1L, 2L);
  }

  @Test
  void noSelectionStillMeansAbstentionForBothGroups() throws Exception {
    hasDelega();
    mvc.perform(vote()).andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Voto.class);
    verify(voti, times(2)).save(saved.capture());
    assertThat(saved.getAllValues()).allSatisfy(voto -> assertThat(voto.getScelte()).containsExactly(3L));
  }

  @Test
  void sparseIndexedSelectionsFromAnOlderFormAreSupported() throws Exception {
    mvc.perform(vote().param("inProprio[2]", "Terza").param("inProprio[3]", "Astenuto"))
        .andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Voto.class);
    verify(voti).save(saved.capture());
    assertThat(saved.getValue().getScelte()).containsExactly(2L, 3L);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Non disponibile", "Prima"})
  void unknownOrRepeatedOptionsAreRejected(String secondChoice) throws Exception {
    mvc.perform(vote().param("inProprio", "Prima", secondChoice))
        .andExpect(model().attributeHasFieldErrors("votoModel", "inProprio"));
    assertNoVoteRecorded();
  }

  @Test
  void singleChoiceDoesNotAcceptTwoOptions() throws Exception {
    votazione.setNumeroScelte(-1L);
    mvc.perform(vote().param("inProprio", "Prima", "Seconda"))
        .andExpect(model().attributeHasFieldErrors("votoModel", "inProprio"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("al massimo 1 opzione.")));
    assertNoVoteRecorded();
  }

  @Test
  void singleChoiceViewUsesSeparateRadioGroupsAndTheEffectiveLimit() throws Exception {
    votazione.setNumeroScelte(-1L);
    hasDelega();
    var html = mvc.perform(get(URL).principal(principal)).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(html).contains("data-max-selected=\"1\"", "name=\"inProprio\"", "name=\"perDelega\"", "/js/voto.js");
    assertThat(html.split("type=\"radio\"", -1)).hasSize(9);
    assertThat(html.split("/js/voto.js", -1)).hasSize(2);
  }

  @Test
  void previewUsesTheSameEffectiveLimit() throws Exception {
    votazione.setNumeroScelte(-1L);
    when(assembleaState.getPresenti(1L)).thenReturn(Set.of());
    var html = mvc.perform(get(URL).principal(principal)).andExpect(view().name("votazioni/preview"))
        .andReturn().getResponse().getContentAsString();
    assertThat(html).containsPattern("Opzione \\(max\\s+1\\s+\\)");
  }

  @Test
  void publicVoteStillRedirectsToResults() throws Exception {
    votazione.setTipoVotazione(TipoVotazione.PALESE);
    mvc.perform(vote().param("inProprio", "Prima"))
        .andExpect(redirectedUrl(URL + "/risultati"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "0", "", "non numerico"})
  void creatingAnInvalidLimitShowsAnErrorWithoutSaving(String limit) throws Exception {
    mvc.perform(post(CREATE_URL).principal(principal).param("quesito", "Nuova votazione")
        .param("scelte", "Prima\nSeconda").param("numeroScelte", limit))
        .andExpect(status().isOk()).andExpect(view().name("votazioni/create"))
        .andExpect(model().attributeHasFieldErrors("votazioneModel", "numeroScelte"));
    verify(votazioni, never()).save(any());
    verifyNoInteractions(messages);
  }

  @Test
  void creatingAValidMultipleChoiceVoteKeepsItsLimit() throws Exception {
    mvc.perform(post(CREATE_URL).principal(principal).param("quesito", "Nuova votazione")
        .param("scelte", "Prima\nSeconda\nTerza").param("numeroScelte", "2"))
        .andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Votazione.class);
    verify(votazioni).save(saved.capture());
    assertThat(saved.getValue().getNumeroScelte()).isEqualTo(2L);
    assertThat(saved.getValue().getScelte()).containsExactly("Prima", "Seconda", "Terza", "Astenuto");
  }

  @Test
  void creatingWithoutALimitDefaultsToSingleChoice() throws Exception {
    mvc.perform(post(CREATE_URL).principal(principal).param("quesito", "Nuova votazione")
        .param("scelte", "Prima\nSeconda"))
        .andExpect(redirectedUrl("/assemblea/1"));
    var saved = ArgumentCaptor.forClass(Votazione.class);
    verify(votazioni).save(saved.capture());
    assertThat(saved.getValue().getNumeroScelte()).isEqualTo(1L);
  }
}
