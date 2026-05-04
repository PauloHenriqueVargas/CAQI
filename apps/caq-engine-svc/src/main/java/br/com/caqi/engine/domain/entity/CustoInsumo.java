package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "custo_insumo")
@Getter
@Setter
@NoArgsConstructor
public class CustoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custo_id")
    private Long id;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(name = "custo_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "fonte_preco", length = 50)
    private String fontePreco;

    @Column(name = "indice_atualizacao", length = 50)
    private String indiceAtualizacao;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;
}
