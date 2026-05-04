package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "despesa")
@Getter
@Setter
@NoArgsConstructor
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "despesa_id")
    private Long id;

    @Column(nullable = false, length = 6)
    private String competencia;

    /** Código natureza despesa (ex.: 3.1.90.11 = pessoal/salários, 4.4.90.52 = capital/equipamentos) */
    @Column(nullable = false, length = 20)
    private String natureza;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "fonte_recurso_id")
    private Long fonteRecursoId;

    @Column(length = 20)
    private String pcasp;

    /** Marca despesa para SIOPE (ex.: "MDE", "FUNDEB", "Geral") */
    @Column(name = "siope_grupo", length = 60)
    private String siopeGrupo;

    @Column(name = "contrato_id")
    private Long contratoId;

    /** True se a natureza começa com '3.1' (Pessoal e Encargos Sociais) */
    public boolean isPessoal() {
        return natureza != null && natureza.startsWith("3.1");
    }

    /** True se a natureza começa com '4' (Investimentos / Inversões — capital) */
    public boolean isCapital() {
        return natureza != null && natureza.startsWith("4");
    }
}
