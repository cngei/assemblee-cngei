package it.cngei.assemblee.entities;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import it.cngei.assemblee.enums.TipoVotazione;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
public class Votazione {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long idAssemblea;
  private TipoVotazione tipoVotazione;
  private boolean aperta;
  private boolean terminata;
  private boolean statutaria;
  private Long numeroScelte;
  private Long presenti;
  private Long quorum;
  private Long quorumPerOpzione;

  private String quesito;
  private String descrizione;

  @OneToMany(mappedBy = "idVotazione")
  private List<Voto> voti;

  @Column(name = "scelte", columnDefinition = "varchar[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] scelte;
}
