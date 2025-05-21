package it.cngei.assemblee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Data
@Table
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Mozione {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private Long idAssemblea;
  private String titolo;
  @Column(name = "testo", columnDefinition = "text")
  private String testo;

  @Column(name = "firmatari", columnDefinition = "bigint[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private Long[] firmatari;
}
