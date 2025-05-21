package it.cngei.assemblee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Voto {
  @Id
  private String id;

  private Long idVotazione;
  private boolean perDelega;

  @Column(name = "scelte", columnDefinition = "bigint[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Long[] scelte;
}
