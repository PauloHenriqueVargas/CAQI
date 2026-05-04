package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parametro_etapa")
@Getter
@Setter
@NoArgsConstructor
public class ParametroEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parametro_id")
    private Long id;

    @Column(name = "etapa_id", nullable = false)
    private Long etapaId;

    @Column(length = 20)
    private String tempo;

    @Column(name = "alunos_por_turma", precision = 6, scale = 2)
    private BigDecimal alunosPorTurma;

    @Column(name = "carga_horaria_docente_h_sem", precision = 6, scale = 2)
    private BigDecimal cargaHorariaDocenteHSem;

    @Column(name = "jornada_dias_ano", precision = 6, scale = 2)
    private BigDecimal jornadaDiasAno;

    @Column(name = "coef_rural", precision = 6, scale = 3)
    private BigDecimal coefRural;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;
}
