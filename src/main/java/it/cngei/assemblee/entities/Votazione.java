package it.cngei.assemblee.entities;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import it.cngei.assemblee.enums.TipoVotazione;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.List;

@Data
@Builder
@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
    @TypeDef(
        name = "string-array",
        typeClass = StringArrayType.class
    )
})
public class Votazione {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long idAssemblea;
  private TipoVotazione tipoVotazione;
  private boolean terminata;
  private Long numeroScelte;
  private Long presenti;

  private String quesito;

  @OneToMany(mappedBy = "idVotazione")
  private List<Voto> voti;

  @Column(name = "scelte", columnDefinition = "varchar[]")
  @Type(type = "string-array")
  private String[] scelte;
}
