package it.cngei.assemblee.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VotazioneEditModel {
  private String quesito;
  private String descrizione;
  private String scelte;
  private Long numeroScelte;
  private boolean votoPalese;
  private Long quorum = 50L;
  private Long quorumPerOpzione;
  private boolean aperta;
  private boolean statutaria;
}
