package com.materimperium.backendtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import com.materimperium.backendtest.repository.ResumoRegistroRepository;
import com.materimperium.backendtest.service.ProcessamentoAsyncService;
import com.materimperium.backendtest.support.IntegrationTestSupport;
import com.materimperium.backendtest.support.SyncAsyncTestConfig;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(SyncAsyncTestConfig.class)
class ProcessamentoAsyncServiceTest extends IntegrationTestSupport {

    @Autowired
    private ProcessamentoAsyncService processamentoAsyncService;

    @Autowired
    private ArquivoProcessamentoRepository arquivoProcessamentoRepository;

    @Autowired
    private ResumoRegistroRepository resumoRegistroRepository;

    @Test
    void deveMarcarProcessamentoComErroQuandoArquivoTemporarioNaoExiste() {
        ArquivoProcessamento arquivo = new ArquivoProcessamento();
        arquivo.setNomeArquivo("faltando.txt");
        arquivo.setStatus(StatusProcessamento.EM_PROCESSAMENTO);
        arquivo.setCriadoEm(Instant.now());
        arquivo.setCaminhoTemporario("target/test-temp/arquivo-inexistente.txt");
        arquivo = arquivoProcessamentoRepository.save(arquivo);
        var arquivoId = arquivo.getId();

        processamentoAsyncService.processarArquivo(arquivoId, "test-correlation-id");

        ArquivoProcessamento atualizado = aguardar(
                () -> arquivoProcessamentoRepository.findById(arquivoId).orElseThrow(),
                arquivoProcessamento -> arquivoProcessamento.getStatus() != StatusProcessamento.EM_PROCESSAMENTO
        );
        assertThat(atualizado.getStatus()).isEqualTo(StatusProcessamento.FINALIZADO_COM_ERROS);
        assertThat(atualizado.getMensagemErro()).isNotBlank();
        assertThat(atualizado.getFinalizadoEm()).isNotNull();
        assertThat(atualizado.getCaminhoTemporario()).isEmpty();
        assertThat(resumoRegistroRepository.findAllByArquivoProcessamentoIdOrderByRegistroAsc(arquivoId)).isEmpty();
    }

    private ArquivoProcessamento aguardar(
            Supplier<ArquivoProcessamento> supplier,
            java.util.function.Predicate<ArquivoProcessamento> condicao
    ) {
        ArquivoProcessamento atual = supplier.get();
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            if (condicao.test(atual)) {
                return atual;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            atual = supplier.get();
        }
        return atual;
    }
}
