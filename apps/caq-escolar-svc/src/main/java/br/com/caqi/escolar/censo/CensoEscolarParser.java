package br.com.caqi.escolar.censo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Consumer;

/**
 * Lê o arquivo ESCOLAS_*.csv do INEP em streaming (sem carregar em memória).
 *
 * Formato:
 *  - Codificação: Latin-1 (ISO-8859-1) — INEP publica assim historicamente
 *  - Separador: ';'
 *  - Header: primeira linha
 *
 * Em paralelo ao parsing, calcula SHA-256 do conteúdo bruto e conta bytes.
 * Isso garante idempotência (mesma versão de ESCOLAS.csv não é importada
 * duas vezes) e permite registrar o tamanho do arquivo na auditoria.
 */
@Component
public class CensoEscolarParser {

    /** Charset padrão dos microdados INEP. */
    public static final Charset CENSO_CHARSET = Charset.forName("ISO-8859-1");

    /** Separador padrão dos microdados INEP. */
    public static final char CENSO_SEPARATOR = ';';

    private final CsvMapper mapper;

    public CensoEscolarParser() {
        this.mapper = new CsvMapper();
        // Tolera linhas com colunas faltando ou em ordem diferente do POJO.
        this.mapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        this.mapper.configure(CsvParser.Feature.SKIP_EMPTY_LINES, true);
        this.mapper.configure(CsvParser.Feature.TRIM_SPACES, true);
        this.mapper.addMixIn(CensoEscolaRecord.class, IgnoreUnknown.class);
    }

    /**
     * Parse streaming. Aplica {@code consumer} a cada record.
     *
     * @return resultado com hash SHA-256 do arquivo, total de bytes lidos e
     *         total de linhas processadas (excluindo header).
     */
    public ResultadoParse parse(InputStream raw, Consumer<CensoEscolaRecord> consumer) throws IOException {
        final MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }

        ContadorBytes contador = new ContadorBytes(new DigestInputStream(raw, sha256));
        try (Reader reader = new InputStreamReader(contador, CENSO_CHARSET)) {

            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(CENSO_SEPARATOR);

            int linhas = 0;
            try (var iter = mapper
                    .readerFor(CensoEscolaRecord.class)
                    .with(schema)
                    .<CensoEscolaRecord>readValues(reader)) {

                while (iter.hasNext()) {
                    CensoEscolaRecord rec = iter.next();
                    if (rec == null) continue;
                    consumer.accept(rec);
                    linhas++;
                }
            }

            String hashHex = HexFormat.of().formatHex(sha256.digest());
            return new ResultadoParse(linhas, hashHex, contador.totalBytes);
        }
    }

    public record ResultadoParse(int linhas, String hashSha256Hex, long bytesLidos) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IgnoreUnknown {}

    /** Wrapper que conta bytes consumidos para registrar tamanho do arquivo. */
    private static final class ContadorBytes extends FilterInputStream {
        long totalBytes = 0;

        ContadorBytes(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) totalBytes++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) totalBytes += n;
            return n;
        }
    }
}
