package com.materimperium.backendtest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.materimperium.backendtest.service.ProcessamentoAsyncService;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadProcessingInProgressIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessamentoAsyncService processamentoAsyncService;

    @BeforeEach
    void setup() {
        doNothing().when(processamentoAsyncService).processarArquivo(any(UUID.class));
    }

    @Test
    void deveRetornar409QuandoResultadoForConsultadoDuranteProcessamento() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String id = uploadJson.get("id").asText();

        mockMvc.perform(get("/api/uploads/{id}/status", id)
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));

        mockMvc.perform(get("/api/uploads/{id}/resultado", id)
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    }
}
