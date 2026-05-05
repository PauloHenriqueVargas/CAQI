package br.com.caqi.escolar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "censo_matricula_resumo")
@Getter
@Setter
@NoArgsConstructor
public class CensoMatriculaResumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resumo_id")
    private Long id;

    @Column(name = "importacao_id", nullable = false)
    private Long importacaoId;

    @Column(name = "ano_censo", nullable = false)
    private Integer anoCenso;

    @Column(name = "escola_inep", nullable = false, length = 20)
    private String escolaInep;

    @Column(name = "escola_id")
    private Long escolaId;

    @Column(name = "etapa_codigo", nullable = false, length = 30)
    private String etapaCodigo;

    @Column(name = "qtd_alunos", nullable = false)
    private Integer qtdAlunos;
}
