package com.materimperium.backendtest.support;

import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import com.materimperium.backendtest.repository.ResumoRegistroRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class IntegrationTestSupport {

    @Autowired
    protected ArquivoProcessamentoRepository arquivoProcessamentoRepository;

    @Autowired
    protected ResumoRegistroRepository resumoRegistroRepository;

    @AfterEach
    void cleanup() throws IOException {
        resumoRegistroRepository.deleteAll();
        arquivoProcessamentoRepository.deleteAll();

        Path tempDir = Path.of("target/test-temp");
        if (Files.exists(tempDir)) {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    protected String arquivoValido() {
        return """
                |0000|017|0|01012026|31012026|
                |0001|0|
                |C170|item-a|10|
                |C170|item-b|20|
                |C100|nota-1|
                |C190|resumo-1|
                """;
    }
}
