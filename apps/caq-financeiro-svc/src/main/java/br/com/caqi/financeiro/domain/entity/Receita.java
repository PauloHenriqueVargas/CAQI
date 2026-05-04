package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "receita")
@Getter
@Setter
@NoArgsConstructor
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receita_id")
    private Long id;

    /** YYYYMM (CHAR 6) */
    @Column(nullable = false, length = 6)
    private String competencia;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    /** impostos | transferencias | Fundeb_VAAF | Fundeb_VAAT | Fundeb_VAAR | outras */
    @Column(length = 50)
    private String origem;

    @Column(length = 20)
    private String pcasp;
}
