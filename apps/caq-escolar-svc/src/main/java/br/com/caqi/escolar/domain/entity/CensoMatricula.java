package br.com.caqi.escolar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "censo_matricula")
@Getter
@Setter
@NoArgsConstructor
public class CensoMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matricula_censo_id")
    private Long id;

    @Column(name = "importacao_id", nullable = false)
    private Long importacaoId;

    @Column(name = "ano_censo", nullable = false)
    private Integer anoCenso;

    @Column(name = "id_matricula_inep", nullable = false, length = 20)
    private String idMatriculaInep;

    @Column(name = "id_aluno_hash", nullable = false, length = 64)
    private String idAlunoHash;

    @Column(name = "escola_inep", nullable = false, length = 20)
    private String escolaInep;

    @Column(name = "escola_id")
    private Long escolaId;

    @Column(name = "etapa_codigo", nullable = false, length = 30)
    private String etapaCodigo;

    @Column(name = "tp_etapa_ensino")
    private Short tpEtapaEnsino;

    private Short idade;

    @Column(name = "tp_sexo")
    private Short tpSexo;

    @Column(name = "tp_cor_raca")
    private Short tpCorRaca;

    @Column(name = "tp_zona_residencial")
    private Short tpZonaResidencial;

    @Column(name = "in_necessidade_especial", nullable = false)
    private Boolean inNecessidadeEspecial = false;

    @Column(name = "necessidades_codigos")
    private String necessidadesCodigos;
}
