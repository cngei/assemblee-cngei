package it.cngei.assemblee.dtos;

import it.cngei.assemblee.enums.TipoMessaggio;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageModel {
  private Long idAssemblea;
  private TipoMessaggio tipoMessaggio;
}
