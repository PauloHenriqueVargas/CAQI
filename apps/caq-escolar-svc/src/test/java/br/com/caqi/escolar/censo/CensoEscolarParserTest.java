package br.com.caqi.escolar.censo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CensoEscolarParser — parsing streaming Latin-1 / ; / SHA-256")
class CensoEscolarParserTest {

    private final CensoEscolarParser parser = new CensoEscolarParser();

    @Test
    @DisplayName("Parseia o fixture e devolve 8 registros + hash determinístico")
    void parse_fixture8registros() throws Exception {
        List<CensoEscolaRecord> rec = new ArrayList<>();
        var resultado = parser.parse(fixture(), rec::add);

        assertThat(resultado.linhas()).isEqualTo(8);
        assertThat(rec).hasSize(8);
        assertThat(resultado.hashSha256Hex()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(resultado.bytesLidos()).isGreaterThan(100);
    }

    @Test
    @DisplayName("Hash é estável entre execuções idênticas (idempotência)")
    void hash_estavel() throws Exception {
        var r1 = parser.parse(fixture(), r -> {});
        var r2 = parser.parse(fixture(), r -> {});
        assertThat(r1.hashSha256Hex()).isEqualTo(r2.hashSha256Hex());
    }

    @Test
    @DisplayName("Mapeia campos QT_MAT_* corretamente para EMEF Castro Alves (registro 2)")
    void mapeia_campos_corretamente() throws Exception {
        List<CensoEscolaRecord> rec = new ArrayList<>();
        parser.parse(fixture(), rec::add);

        var castroAlves = rec.stream()
                .filter(r -> "17000002".equals(r.coEntidade()))
                .findFirst()
                .orElseThrow();
        assertThat(castroAlves.noEntidade()).isEqualTo("EMEF Castro Alves");
        assertThat(castroAlves.coMunicipio()).isEqualTo("1721000");
        assertThat(castroAlves.tpDependencia()).isEqualTo(3);
        assertThat(castroAlves.qtMatFundAi()).isEqualTo(380);
        assertThat(castroAlves.qtMatFundAf()).isEqualTo(320);
        assertThat(castroAlves.qtMatEja()).isEqualTo(15);
        assertThat(castroAlves.qtMatInfCre()).isZero();
        assertThat(castroAlves.ehMunicipal()).isTrue();
        assertThat(castroAlves.emAtividade()).isTrue();
        assertThat(castroAlves.localizacaoTexto()).isEqualTo("urbana");
    }

    @Test
    @DisplayName("Acentos Latin-1 preservados (Maria José, Pequeno Príncipe, Vinícius)")
    void latin1_acentos_preservados() throws Exception {
        List<CensoEscolaRecord> rec = new ArrayList<>();
        parser.parse(fixture(), rec::add);

        assertThat(rec).extracting(CensoEscolaRecord::noEntidade)
                .anyMatch(s -> s.equals("EMEI Pequeno Príncipe"))
                .anyMatch(s -> s.contains("Vinícius"))
                .anyMatch(s -> s.contains("Maria José"))
                .anyMatch(s -> s.contains("Conceição"));
    }

    @Test
    @DisplayName("Helpers de classificação (ehMunicipal, emAtividade, localizacaoTexto)")
    void helpers_classificacao() throws Exception {
        List<CensoEscolaRecord> rec = new ArrayList<>();
        parser.parse(fixture(), rec::add);

        var estadual = rec.stream().filter(r -> "17000004".equals(r.coEntidade())).findFirst().orElseThrow();
        assertThat(estadual.ehMunicipal()).isFalse();
        assertThat(estadual.emAtividade()).isTrue();

        var privada = rec.stream().filter(r -> "17000005".equals(r.coEntidade())).findFirst().orElseThrow();
        assertThat(privada.ehMunicipal()).isFalse();

        var extinta = rec.stream().filter(r -> "17000007".equals(r.coEntidade())).findFirst().orElseThrow();
        assertThat(extinta.ehMunicipal()).isTrue();
        assertThat(extinta.emAtividade()).isFalse();

        var rural = rec.stream().filter(r -> "17000003".equals(r.coEntidade())).findFirst().orElseThrow();
        assertThat(rural.localizacaoTexto()).isEqualTo("rural");
    }

    @Test
    @DisplayName("CSV inválido (header faltando colunas obrigatórias) → lê o que conseguir")
    void csv_minimo_funciona() throws Exception {
        String mini = "NU_ANO_CENSO;CO_ENTIDADE;NO_ENTIDADE;CO_MUNICIPIO;TP_DEPENDENCIA;TP_SITUACAO_FUNCIONAMENTO\n"
                    + "2024;99999999;Escola Mínima;1721000;3;1\n";
        List<CensoEscolaRecord> rec = new ArrayList<>();
        var res = parser.parse(new java.io.ByteArrayInputStream(mini.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)),
                                rec::add);
        assertThat(res.linhas()).isEqualTo(1);
        assertThat(rec.get(0).coEntidade()).isEqualTo("99999999");
        assertThat(rec.get(0).qtMatFundAi()).isNull(); // não estava no header
    }

    @Test
    @DisplayName("Stream vazio retorna 0 linhas + hash do empty (e2c569be17396eda...)")
    void stream_vazio_zero_linhas() throws Exception {
        var rec = new ArrayList<CensoEscolaRecord>();
        var res = parser.parse(new java.io.ByteArrayInputStream(new byte[0]), rec::add);
        assertThat(res.linhas()).isZero();
        assertThat(rec).isEmpty();
        // SHA-256 do byte-array vazio
        assertThat(res.hashSha256Hex()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    private InputStream fixture() {
        var s = getClass().getResourceAsStream("/censo/ESCOLAS_FIXTURE.csv");
        if (s == null) throw new IllegalStateException("Fixture /censo/ESCOLAS_FIXTURE.csv não encontrado no classpath");
        return s;
    }
}
