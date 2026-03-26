package com.materimperium.backendtest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.materimperium.backendtest.service.ProcessamentoAsyncService;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorrelationIdIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessamentoAsyncService processamentoAsyncService;

    @Test
    void deveGerarCorrelationIdQuandoHeaderNaoForInformado() throws Exception {
        doNothing().when(processamentoAsyncService).processarArquivo(any(UUID.class), any());

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
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void deveReaproveitarCorrelationIdInformadoPeloCliente() throws Exception {
        doNothing().when(processamentoAsyncService).processarArquivo(any(UUID.class), any());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio")
                        .header("X-Correlation-Id", "req-123"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-Id", "req-123"));
    }
}
