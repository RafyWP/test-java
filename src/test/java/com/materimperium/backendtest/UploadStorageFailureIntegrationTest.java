package com.materimperium.backendtest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.materimperium.backendtest.support.IntegrationTestSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.storage.temp-dir=target/test-storage-blocker")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadStorageFailureIntegrationTest extends IntegrationTestSupport {

    private static final Path BLOQUEADOR = Path.of("target/test-storage-blocker");

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void prepararArquivoBloqueador() throws IOException {
        Files.createDirectories(BLOQUEADOR.getParent());
        Files.writeString(BLOQUEADOR, "bloqueia-create-directories");
    }

    @AfterEach
    void removerArquivoBloqueador() throws IOException {
        Files.deleteIfExists(BLOQUEADOR);
    }

    @Test
    void deveRetornar500QuandoFalharAoArmazenarArquivoTemporario() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.erro").value("Nao foi possivel armazenar o arquivo para processamento"));
    }
}
