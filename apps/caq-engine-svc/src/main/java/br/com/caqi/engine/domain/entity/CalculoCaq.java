package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculo_caq",
        uniqueConstraints = @UniqueConstraint(columnNames = {"etapa_id", "escola_id", "ano"}))
@Getter
@Setter
@NoArgsConstructor
public class CalculoCaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calculo_id")
    private Long id;

    @Column(name = "etapa_id", nullable = false)
    private Long etapaId;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(nullable = false)
    private Integer ano;

    @Column(name = "valor_caqi_aluno_ano", precision = 14, scale = 2)
    private BigDecimal valorCaqiAlunoAno;

    @Column(name = "valor_caq_aluno_ano", precision = 14, scale = 2)
    private BigDecimal valorCaqAlunoAno;

    @Column(name = "gap_execucao", precision = 14, scale = 2)
    private BigDecimal gapExecucao;

    @OneToMany(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalculoCaqItem> itens = new ArrayList<>();

    public void addItem(CalculoCaqItem item) {
        item.setCalculo(this);
        itens.add(item);
    }
}
