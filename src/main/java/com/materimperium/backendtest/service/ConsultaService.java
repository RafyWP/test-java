package com.materimperium.backendtest.service;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.dto.ResultadoResponse;
import com.materimperium.backendtest.dto.ResumoItemResponse;
import com.materimperium.backendtest.dto.StatusResponse;
import com.materimperium.backendtest.exception.ProcessamentoEmAndamentoException;
import com.materimperium.backendtest.exception.RecursoNaoEncontradoException;
import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import com.materimperium.backendtest.repository.ResumoRegistroRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private final ArquivoProcessamentoRepository arquivoProcessamentoRepository;
    private final ResumoRegistroRepository resumoRegistroRepository;

    public ConsultaService(
            ArquivoProcessamentoRepository arquivoProcessamentoRepository,
            ResumoRegistroRepository resumoRegistroRepository
    ) {
        this.arquivoProcessamentoRepository = arquivoProcessamentoRepository;
        this.resumoRegistroRepository = resumoRegistroRepository;
    }

    public StatusResponse consultarStatus(UUID id) {
        ArquivoProcessamento arquivo = buscarArquivo(id);
        return new StatusResponse(arquivo.getId(), arquivo.getStatus());
    }

    public ResultadoResponse consultarResultado(UUID id) {
        ArquivoProcessamento arquivo = buscarArquivo(id);
        if (arquivo.getStatus() == StatusProcessamento.EM_PROCESSAMENTO) {
            throw new ProcessamentoEmAndamentoException("Arquivo ainda em processamento");
        }

        List<ResumoItemResponse> resumo = resumoRegistroRepository.findAllByArquivoProcessamentoIdOrderByRegistroAsc(id)
                .stream()
                .map(item -> new ResumoItemResponse(item.getRegistro(), item.getTotal()))
                .toList();

        return new ResultadoResponse(arquivo.getStatus(), resumo);
    }

    private ArquivoProcessamento buscarArquivo(UUID id) {
        return arquivoProcessamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo nao encontrado"));
    }
}
