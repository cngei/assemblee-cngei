package it.cngei.assemblee.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VotazioneEditModel {
  private String quesito;
  private String scelte;
  private Long numeroScelte;
  private boolean votoPalese;
}
