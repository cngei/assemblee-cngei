package it.cngei.assemblee.dtos;

import lombok.Data;

import java.util.ArrayList;

@Data
public class VotoEditModel {
  private ArrayList<String> inProprio;
  private ArrayList<String> perDelega;
  private String idProprio;
  private String idDelega;
}
