package com.materimperium.backendtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.ResumoRegistro;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.service.ProcessamentoAsyncService;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
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
class UploadApiContractTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessamentoAsyncService processamentoAsyncService;

    @BeforeEach
    void setup() {
        doNothing().when(processamentoAsyncService).processarArquivo(any(UUID.class), any());
    }

    @Test
    void deveRetornarContratoExatoDeUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        MvcResult response = mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(json.isObject()).isTrue();
        assertThat(fieldNames(json)).containsExactly("id");
        assertThatCodeHasUuid(json.get("id").asText());
    }

    @Test
    void deveRetornarContratoExatoDeStatus() throws Exception {
        ArquivoProcessamento arquivo = novoArquivo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
        arquivo = arquivoProcessamentoRepository.save(arquivo);

        MvcResult response = mockMvc.perform(get("/api/uploads/{id}/status", arquivo.getId())
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(json.isObject()).isTrue();
        assertThat(fieldNames(json)).containsExactly("id", "status");
        assertThat(json.get("id").asText()).isEqualTo(arquivo.getId().toString());
        assertThat(json.get("status").asText()).isEqualTo("FINALIZADO_COM_SUCESSO");
    }

    @Test
    void deveRetornarContratoExatoDeResultadoFinalizado() throws Exception {
        ArquivoProcessamento arquivo = novoArquivo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
        arquivo = arquivoProcessamentoRepository.save(arquivo);

        resumoRegistroRepository.save(novoResumo(arquivo, "0000", 1L));
        resumoRegistroRepository.save(novoResumo(arquivo, "0001", 1L));
        resumoRegistroRepository.save(novoResumo(arquivo, "C170", 3L));

        mockMvc.perform(get("/api/uploads/{id}/resultado", arquivo.getId())
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                        {
                          "status": "FINALIZADO_COM_SUCESSO",
                          "resumo": [
                            { "registro": "0000", "total": 1 },
                            { "registro": "0001", "total": 1 },
                            { "registro": "C170", "total": 3 }
                          ]
                        }
                        """,
                        true
                ));
    }

    @Test
    void deveRetornarContratoExatoDeResultadoEmProcessamento() throws Exception {
        ArquivoProcessamento arquivo = novoArquivo(StatusProcessamento.EM_PROCESSAMENTO);
        arquivo = arquivoProcessamentoRepository.save(arquivo);

        mockMvc.perform(get("/api/uploads/{id}/resultado", arquivo.getId())
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isConflict())
                .andExpect(content().json(
                        """
                        {
                          "id": null,
                          "status": "EM_PROCESSAMENTO"
                        }
                        """,
                        true
                ));
    }

    @Test
    void deveRetornarContratoExatoDeErro() throws Exception {
        mockMvc.perform(get("/api/uploads/{id}/status", "id-invalido")
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        """
                        {
                          "erro": "Parametro invalido"
                        }
                        """,
                        true
                ));
    }

    private ArquivoProcessamento novoArquivo(StatusProcessamento status) {
        ArquivoProcessamento arquivo = new ArquivoProcessamento();
        arquivo.setNomeArquivo("arquivo.txt");
        arquivo.setStatus(status);
        arquivo.setCriadoEm(Instant.now());
        arquivo.setCaminhoTemporario("");
        return arquivo;
    }

    private ResumoRegistro novoResumo(ArquivoProcessamento arquivo, String registro, Long total) {
        ResumoRegistro resumo = new ResumoRegistro();
        resumo.setArquivoProcessamento(arquivo);
        resumo.setRegistro(registro);
        resumo.setTotal(total);
        return resumo;
    }

    private Set<String> fieldNames(JsonNode json) {
        Iterator<String> iterator = json.fieldNames();
        java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>();
        iterator.forEachRemaining(fields::add);
        return fields;
    }

    private void assertThatCodeHasUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
