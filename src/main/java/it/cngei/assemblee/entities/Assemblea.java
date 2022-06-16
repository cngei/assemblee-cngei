package it.cngei.assemblee.entities;

import com.vladmihalcea.hibernate.type.array.LongArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Table
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
    @TypeDef(
        name = "long-array",
        typeClass = LongArrayType.class
    ),
    @TypeDef(
        name = "string-array",
        typeClass = StringArrayType.class
    )
})
public class Assemblea {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String nome;
  private String descrizione;
  private Long idProprietario;
  private Long idPresidente;
  private Long idCovepo;
  private LocalDateTime convocazione;
  private LocalDateTime fine;
  private boolean inCorso;
  private Long stepOdg;
  private boolean require2FA;
  private boolean isNazionale;
  private boolean isMozioniOpen;

  @Column(name = "covepo", columnDefinition = "bigint[]")
  @Type(type = "long-array")
  private Long[] covepo;

  @Column(name = "partecipanti", columnDefinition = "bigint[]")
  @Type(type = "long-array")
  private Long[] partecipanti;

  @Column(name = "odg", columnDefinition = "varchar[]")
  @Type(type = "string-array")
  private String[] odg;

  @OneToMany(mappedBy = "idAssemblea")
  private List<Mozione> mozioni;
}
