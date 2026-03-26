package com.materimperium.backendtest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import com.materimperium.backendtest.support.SyncAsyncTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SyncAsyncTestConfig.class)
class UploadApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveExecutarFluxoCompletoDeUploadStatusEResultado() throws Exception {
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String id = uploadJson.get("id").asText();

        mockMvc.perform(get("/api/uploads/{id}/status", id)
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("FINALIZADO_COM_SUCESSO"));

        mockMvc.perform(get("/api/uploads/{id}/resultado", id)
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO_COM_SUCESSO"))
                .andExpect(jsonPath("$.resumo[0].registro").value("0000"))
                .andExpect(jsonPath("$.resumo[0].total").value(1))
                .andExpect(jsonPath("$.resumo[1].registro").value("0001"))
                .andExpect(jsonPath("$.resumo[1].total").value(1))
                .andExpect(jsonPath("$.resumo[2].registro").value("C100"))
                .andExpect(jsonPath("$.resumo[2].total").value(1))
                .andExpect(jsonPath("$.resumo[3].registro").value("C170"))
                .andExpect(jsonPath("$.resumo[3].total").value(2))
                .andExpect(jsonPath("$.resumo[4].registro").value("C190"))
                .andExpect(jsonPath("$.resumo[4].total").value(1));
    }
}
