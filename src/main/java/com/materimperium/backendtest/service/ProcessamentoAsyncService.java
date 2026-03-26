package com.materimperium.backendtest.service;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.ResumoRegistro;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.exception.RecursoNaoEncontradoException;
import com.materimperium.backendtest.logging.LoggingContextKeys;
import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import com.materimperium.backendtest.repository.ResumoRegistroRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProcessamentoAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoAsyncService.class);

    private final ArquivoProcessamentoRepository arquivoProcessamentoRepository;
    private final ResumoRegistroRepository resumoRegistroRepository;

    public ProcessamentoAsyncService(
            ArquivoProcessamentoRepository arquivoProcessamentoRepository,
            ResumoRegistroRepository resumoRegistroRepository
    ) {
        this.arquivoProcessamentoRepository = arquivoProcessamentoRepository;
        this.resumoRegistroRepository = resumoRegistroRepository;
    }

    @Async("processamentoExecutor")
    public void processarArquivo(UUID arquivoId, String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(LoggingContextKeys.CORRELATION_ID, correlationId);
        }
        MDC.put(LoggingContextKeys.UPLOAD_ID, arquivoId.toString());
        ArquivoProcessamento arquivo = null;
        Path caminho = null;
        try {
            arquivo = arquivoProcessamentoRepository.findById(arquivoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo nao encontrado"));

            caminho = Path.of(arquivo.getCaminhoTemporario());
            arquivo.setIniciadoEm(Instant.now());
            arquivoProcessamentoRepository.save(arquivo);
            log.info("Processamento iniciado arquivo={} caminhoTemporario={}", arquivo.getNomeArquivo(), caminho);

            Map<String, Long> contagem = contarRegistros(caminho);
            for (Map.Entry<String, Long> entry : contagem.entrySet()) {
                ResumoRegistro resumo = new ResumoRegistro();
                resumo.setArquivoProcessamento(arquivo);
                resumo.setRegistro(entry.getKey());
                resumo.setTotal(entry.getValue());
                resumoRegistroRepository.save(resumo);
            }

            arquivo.setStatus(StatusProcessamento.FINALIZADO_COM_SUCESSO);
            arquivo.setFinalizadoEm(Instant.now());
            arquivo.setMensagemErro(null);
            log.info("Processamento finalizado com sucesso registrosDistintos={}", contagem.size());
        } catch (Exception ex) {
            if (arquivo != null) {
                arquivo.setStatus(StatusProcessamento.FINALIZADO_COM_ERROS);
                arquivo.setFinalizadoEm(Instant.now());
                arquivo.setMensagemErro(ex.getMessage());
            }
            log.error("Processamento finalizado com erro", ex);
        } finally {
            if (caminho != null) {
                try {
                    Files.deleteIfExists(caminho);
                } catch (IOException ignored) {
                }
            }
            if (arquivo != null) {
                arquivo.setCaminhoTemporario("");
                arquivoProcessamentoRepository.save(arquivo);
            }
            MDC.remove(LoggingContextKeys.UPLOAD_ID);
            MDC.remove(LoggingContextKeys.CORRELATION_ID);
        }
    }

    private Map<String, Long> contarRegistros(Path caminho) throws IOException {
        Map<String, Long> contagem = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.isBlank()) {
                    continue;
                }

                String[] partes = linha.split("\\|", -1);
                if (partes.length > 1 && !partes[1].isBlank()) {
                    contagem.merge(partes[1], 1L, Long::sum);
                }
            }
        }
        return contagem;
    }
}
