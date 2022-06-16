package it.cngei.assemblee.services;

import it.cngei.assemblee.dtos.MozioneEditModel;
import it.cngei.assemblee.entities.Mozione;
import it.cngei.assemblee.entities.Votazione;
import it.cngei.assemblee.enums.TipoVotazione;
import it.cngei.assemblee.repositories.MozioniRepository;
import it.cngei.assemblee.repositories.SocioRepository;
import it.cngei.assemblee.repositories.VotazioneRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class MozioniService {
  private final MozioniRepository mozioniRepository;
  private final VotazioneRepository votazioneRepository;
  private final SocioRepository socioRepository;

  public MozioniService(MozioniRepository mozioniRepository, VotazioneRepository votazioneRepository, SocioRepository socioRepository) {
    this.mozioniRepository = mozioniRepository;
    this.votazioneRepository = votazioneRepository;
    this.socioRepository = socioRepository;
  }

  public Collection<Mozione> getByAssemblea(Long idAssemblea) {
    return mozioniRepository.findAllByIdAssemblea(idAssemblea);
  }

  public void create(MozioneEditModel model, Long idAssemblea, Long idUtente) {
    var newMozione = Mozione.builder()
        .titolo(model.getTitolo())
        .testo(model.getTesto())
        .idAssemblea(idAssemblea)
        .firmatari(new Long[]{idUtente})
        .build();
    mozioniRepository.save(newMozione);
  }

  public void firma(Long idMozione, Long idUtente) {
    var mozione = getMozione(idMozione);
    if (Arrays.asList(mozione.getFirmatari()).contains(idUtente)) {
      throw new IllegalStateException();
    } else {
      var newFirmatari = Arrays.stream(mozione.getFirmatari()).collect(Collectors.toList());
      newFirmatari.add(idUtente);
      mozione.setFirmatari(newFirmatari.toArray(new Long[0]));
      mozioniRepository.save(mozione);
    }
  }

  public void converti(Long idMozione) {
    var mozione = getMozione(idMozione);
    var firmatari = Arrays.stream(mozione.getFirmatari()).map(x -> socioRepository.findById(x).map(y -> y.getNome() + " - " + y.getSezione()).orElse(x.toString())).collect(Collectors.joining(", "));
    var newVotazione = Votazione.builder()
        .idAssemblea(mozione.getIdAssemblea())
        .tipoVotazione(TipoVotazione.PALESE)
        .quesito(mozione.getTitolo())
        .descrizione(mozione.getTesto() + "\nFirmatari: " + firmatari)
        .numeroScelte(1L)
        .scelte(new String[]{"Favorevole", "Contrario", "Astenuto"})
        .build();
    votazioneRepository.save(newVotazione);
    mozioniRepository.delete(mozione);
  }

  private Mozione getMozione(Long idMozione) {
    var optionalMozione = mozioniRepository.findById(idMozione);
    if (optionalMozione.isEmpty()) {
      throw new NoSuchElementException();
    }
    return optionalMozione.get();
  }
}
