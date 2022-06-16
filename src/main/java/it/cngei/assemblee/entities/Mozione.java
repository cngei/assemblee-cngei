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
    )
})
public class Mozione {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long idAssemblea;
  private String titolo;
  @Column(name = "testo", columnDefinition = "text")
  private String testo;

  @Column(name = "firmatari", columnDefinition = "bigint[]")
  @Type(type = "long-array")
  private Long[] firmatari;
}
