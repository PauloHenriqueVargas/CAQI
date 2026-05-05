package br.com.caqi.escolar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "censo_importacao")
@Getter
@Setter
@NoArgsConstructor
public class CensoImportacao {

    public enum Status { em_andamento, concluida, falhou, simulada }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importacao_id")
    private Long id;

    @Column(name = "ano_censo", nullable = false)
    private Integer anoCenso;

    @Column(name = "arquivo_nome", nullable = false)
    private String arquivoNome;

    @Column(name = "arquivo_hash_sha256", nullable = false, length = 64)
    private String arquivoHashSha256;

    @Column(name = "arquivo_tamanho_bytes")
    private Long arquivoTamanhoBytes;

    @Column(name = "cod_municipio_ibge", nullable = false, length = 7)
    private String codMunicipioIbge;

    @Column(name = "registros_processados", nullable = false)
    private Integer registrosProcessados = 0;

    @Column(name = "registros_municipio", nullable = false)
    private Integer registrosMunicipio = 0;

    @Column(name = "escolas_inseridas", nullable = false)
    private Integer escolasInseridas = 0;

    @Column(name = "escolas_atualizadas", nullable = false)
    private Integer escolasAtualizadas = 0;

    @Column(name = "matriculas_total", nullable = false)
    private Integer matriculasTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.em_andamento;

    @Column(name = "erro_mensagem")
    private String erroMensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "criado_por", nullable = false, length = 60)
    private String criadoPor;
}
