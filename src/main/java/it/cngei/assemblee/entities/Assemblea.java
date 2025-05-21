package it.cngei.assemblee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Table
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Assemblea {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String nome;
    private String descrizione;
    private Long idProprietario;
    private Long idPresidente;
    private LocalDateTime convocazione;
    private LocalDateTime fine;
    private boolean inCorso;
    private Long stepOdg;
    private boolean require2FA;
    private boolean isNazionale;
    private boolean isMozioniOpen;
    private Long totaleDelegati;
    private boolean isInPresenza;

    @Column(name = "covepo", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] covepo;

    @Column(name = "partecipanti", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] partecipanti;

    @Column(name = "odg", columnDefinition = "varchar[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] odg;

    @OneToMany(mappedBy = "idAssemblea")
    private List<Mozione> mozioni;
}
