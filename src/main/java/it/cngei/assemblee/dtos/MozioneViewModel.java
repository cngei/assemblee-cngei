package it.cngei.assemblee.dtos;

import it.cngei.assemblee.entities.Mozione;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class MozioneViewModel {
 private final Long id;
 private final String titolo;
 private final String testo;
 private final boolean isFirmatario;
 private final List<String> firmatari;

 public MozioneViewModel(Mozione mozione, Long idUtente, Function<Long, String> getSocioName) {
  this.id = mozione.getId();
  this.testo = mozione.getTesto();
  this.titolo = mozione.getTitolo();
  this.isFirmatario = Arrays.asList(mozione.getFirmatari()).contains(idUtente);
  this.firmatari = Arrays.stream(mozione.getFirmatari()).map(getSocioName).collect(Collectors.toList());
 }
}
