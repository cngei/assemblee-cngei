package it.cngei.assemblee.dtos;

import lombok.Data;

@Data
public class VotazioneEditModel {
  private String quesito;
  private String descrizione;
  private String scelte;
  private Long numeroScelte = 1L;
  private boolean votoPalese;
  private Long quorum = 50L;
  private Long quorumPerOpzione;
  private boolean aperta;
  private boolean statutaria;
}
