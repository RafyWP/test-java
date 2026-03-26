package com.materimperium.backendtest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import com.materimperium.backendtest.support.SyncAsyncTestConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SyncAsyncTestConfig.class)
class UploadApiErrorIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornar401QuandoNaoEnviarToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Nao autenticado"));
    }

    @Test
    void deveRetornar403QuandoRoleNaoPermiteUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Acesso negado"));
    }

    @Test
    void deveRetornar403QuandoRoleNaoPermiteConsultarResultado() throws Exception {
        ArquivoProcessamento arquivo = novoArquivo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
        arquivo = arquivoProcessamentoRepository.save(arquivo);

        mockMvc.perform(get("/api/uploads/{id}/resultado", arquivo.getId())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Acesso negado"));
    }

    @Test
    void deveRetornar401QuandoTokenForInvalido() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "registros.txt",
                MediaType.TEXT_PLAIN_VALUE,
                arquivoValido().getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-inexistente"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Nao autenticado"));
    }

    @Test
    void deveRetornar400QuandoArquivoEstaVazio() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vazio.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Arquivo vazio"));
    }

    @Test
    void deveRetornar400QuandoPrimeiraLinhaEhInvalida() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalido.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                |9999|abc|
                |0001|0|
                """.getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Primeira linha invalida"));
    }

    @Test
    void deveRetornar400QuandoSegundaLinhaEhInvalida() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalido.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                |0000|017|0|01012026|31012026|
                |0001|1|
                """.getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Segunda linha invalida"));
    }

    @Test
    void deveRetornar400QuandoArquivoTemMenosDeDuasLinhas() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "curto.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "|0000|017|0|01012026|31012026|\n".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Arquivo deve conter ao menos duas linhas"));
    }

    @Test
    void deveRetornar400QuandoArquivoNaoForEnviado() throws Exception {
        mockMvc.perform(multipart("/api/uploads")
                        .with(csrf())
                        .header("Authorization", "Bearer token-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Arquivo nao enviado"));
    }

    @Test
    void deveRetornar404QuandoStatusNaoEncontraArquivo() throws Exception {
        mockMvc.perform(get("/api/uploads/{id}/status", "2f689816-e8dc-4ea2-8d98-f1e8fdd6db33")
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Arquivo nao encontrado"));
    }

    @Test
    void deveRetornar404QuandoResultadoNaoEncontraArquivo() throws Exception {
        mockMvc.perform(get("/api/uploads/{id}/resultado", "2f689816-e8dc-4ea2-8d98-f1e8fdd6db33")
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Arquivo nao encontrado"));
    }

    @Test
    void deveRetornar400QuandoIdForInvalido() throws Exception {
        mockMvc.perform(get("/api/uploads/{id}/status", "id-invalido")
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parametro invalido"));
    }

    @Test
    void deveRetornarResultadoComErroSemResumoQuandoProcessamentoFalhou() throws Exception {
        ArquivoProcessamento arquivo = novoArquivo(StatusProcessamento.FINALIZADO_COM_ERROS);
        arquivo.setMensagemErro("Falha ao ler arquivo temporario");
        arquivo = arquivoProcessamentoRepository.save(arquivo);

        mockMvc.perform(get("/api/uploads/{id}/resultado", arquivo.getId())
                        .header("Authorization", "Bearer token-consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO_COM_ERROS"))
                .andExpect(jsonPath("$.resumo").isArray())
                .andExpect(jsonPath("$.resumo").isEmpty());
    }

    private ArquivoProcessamento novoArquivo(StatusProcessamento status) {
        ArquivoProcessamento arquivo = new ArquivoProcessamento();
        arquivo.setNomeArquivo("arquivo.txt");
        arquivo.setStatus(status);
        arquivo.setCriadoEm(Instant.now());
        arquivo.setCaminhoTemporario("");
        return arquivo;
    }
}
