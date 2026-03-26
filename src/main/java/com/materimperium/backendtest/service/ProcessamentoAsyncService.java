package com.materimperium.backendtest.service;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.ResumoRegistro;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.exception.RecursoNaoEncontradoException;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProcessamentoAsyncService {

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
    public void processarArquivo(UUID arquivoId) {
        ArquivoProcessamento arquivo = arquivoProcessamentoRepository.findById(arquivoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo nao encontrado"));

        Path caminho = Path.of(arquivo.getCaminhoTemporario());
        arquivo.setIniciadoEm(Instant.now());
        arquivoProcessamentoRepository.save(arquivo);

        try {
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
        } catch (Exception ex) {
            arquivo.setStatus(StatusProcessamento.FINALIZADO_COM_ERROS);
            arquivo.setFinalizadoEm(Instant.now());
            arquivo.setMensagemErro(ex.getMessage());
        } finally {
            try {
                Files.deleteIfExists(caminho);
            } catch (IOException ignored) {
            }

            arquivo.setCaminhoTemporario("");
            arquivoProcessamentoRepository.save(arquivo);
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
