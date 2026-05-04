package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "calculo_caq_item")
@Getter
@Setter
@NoArgsConstructor
public class CalculoCaqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_id", nullable = false)
    private CalculoCaq calculo;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(name = "insumo_codigo", nullable = false, length = 30)
    private String insumoCodigo;

    @Column(name = "insumo_nome", nullable = false)
    private String insumoNome;

    @Column(name = "tipo_aplicacao", nullable = false, length = 15)
    private String tipoAplicacao;

    /** "minimo" (entra no CAQi) | "adequado" (entra no CAQ). */
    @Column(nullable = false, length = 20)
    private String perfil;

    @Column(name = "qtd_aplicada", nullable = false, precision = 12, scale = 4)
    private BigDecimal qtdAplicada;

    @Column(name = "custo_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "custo_anual", nullable = false, precision = 14, scale = 4)
    private BigDecimal custoAnual;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal divisor;

    @Column(name = "custo_aluno_ano", nullable = false, precision = 14, scale = 4)
    private BigDecimal custoAlunoAno;

    @Column(name = "base_calculo", nullable = false)
    private String baseCalculo;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
