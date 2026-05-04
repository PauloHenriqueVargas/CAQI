package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fonte_recurso")
@Getter
@Setter
@NoArgsConstructor
public class FonteRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fonte_recurso_id")
    private Long id;

    /** Propria | VAAF | VAAT | VAAR | Outras */
    @Column(nullable = false, length = 30)
    private String tipo;

    private String descricao;
}
