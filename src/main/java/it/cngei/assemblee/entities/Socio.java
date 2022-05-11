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
import java.time.LocalDate;

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
public class Socio {
  @Id
  private Long id;

  private String nome;
  private String sezione;
  private LocalDate dataNascita;
}
