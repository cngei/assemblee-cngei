package it.cngei.assemblee.entities;

import com.vladmihalcea.hibernate.type.array.LongArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
    @TypeDef(
        name = "long-array",
        typeClass = LongArrayType.class
    )
})
public class Voto {
  @Id
  private String id;

  private Long idVotazione;
  private boolean perDelega;

  @Column(name = "scelte", columnDefinition = "bigint[]")
  @Type(type = "long-array")
  private Long[] scelte;
}
