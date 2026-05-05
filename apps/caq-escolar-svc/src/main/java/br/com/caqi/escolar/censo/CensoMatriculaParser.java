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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Consumer;

import static br.com.caqi.escolar.censo.CensoEscolarParser.CENSO_CHARSET;
import static br.com.caqi.escolar.censo.CensoEscolarParser.CENSO_SEPARATOR;

/**
 * Parser específico para MATRICULA_*.csv (microdados aluno-level).
 *
 * Mesmo padrão técnico do {@link CensoEscolarParser} (Latin-1, ';',
 * streaming, SHA-256 + bytes em paralelo), mas mapeando linhas para
 * {@link CensoMatriculaRecord} em vez de {@link CensoEscolaRecord}.
 *
 * Volume: o arquivo MATRICULA_REGIAOXX.csv pode ter dezenas de milhões
 * de linhas. O streaming é essencial — o consumer deve fazer batch
 * insert ao invés de acumular em lista.
 */
@Component
public class CensoMatriculaParser {

    private final CsvMapper mapper;

    public CensoMatriculaParser() {
        this.mapper = new CsvMapper();
        this.mapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        this.mapper.configure(CsvParser.Feature.SKIP_EMPTY_LINES, true);
        this.mapper.configure(CsvParser.Feature.TRIM_SPACES, true);
        this.mapper.addMixIn(CensoMatriculaRecord.class, IgnoreUnknown.class);
    }

    public ResultadoParse parse(InputStream raw, Consumer<CensoMatriculaRecord> consumer) throws IOException {
        final MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }

        ContadorBytes contador = new ContadorBytes(new DigestInputStream(raw, sha256));
        try (Reader reader = new InputStreamReader(contador, CENSO_CHARSET)) {

            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(CENSO_SEPARATOR);

            int linhas = 0;
            try (var iter = mapper
                    .readerFor(CensoMatriculaRecord.class)
                    .with(schema)
                    .<CensoMatriculaRecord>readValues(reader)) {

                while (iter.hasNext()) {
                    CensoMatriculaRecord rec = iter.next();
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
