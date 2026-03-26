package com.materimperium.backendtest.service;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.dto.ResultadoResponse;
import com.materimperium.backendtest.dto.ResumoItemResponse;
import com.materimperium.backendtest.dto.StatusResponse;
import com.materimperium.backendtest.exception.ProcessamentoEmAndamentoException;
import com.materimperium.backendtest.exception.RecursoNaoEncontradoException;
import com.materimperium.backendtest.logging.LoggingContextKeys;
import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import com.materimperium.backendtest.repository.ResumoRegistroRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private static final Logger log = LoggerFactory.getLogger(ConsultaService.class);

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
        MDC.put(LoggingContextKeys.UPLOAD_ID, id.toString());
        try {
            ArquivoProcessamento arquivo = buscarArquivo(id);
            log.info("Consulta de status status={}", arquivo.getStatus());
            return new StatusResponse(arquivo.getId(), arquivo.getStatus());
        } finally {
            MDC.remove(LoggingContextKeys.UPLOAD_ID);
        }
    }

    public ResultadoResponse consultarResultado(UUID id) {
        MDC.put(LoggingContextKeys.UPLOAD_ID, id.toString());
        try {
            ArquivoProcessamento arquivo = buscarArquivo(id);
            if (arquivo.getStatus() == StatusProcessamento.EM_PROCESSAMENTO) {
                log.info("Consulta de resultado com processamento em andamento");
                throw new ProcessamentoEmAndamentoException("Arquivo ainda em processamento");
            }

            List<ResumoItemResponse> resumo = resumoRegistroRepository.findAllByArquivoProcessamentoIdOrderByRegistroAsc(id)
                    .stream()
                    .map(item -> new ResumoItemResponse(item.getRegistro(), item.getTotal()))
                    .toList();

            log.info("Consulta de resultado status={} totalRegistros={}", arquivo.getStatus(), resumo.size());
            return new ResultadoResponse(arquivo.getStatus(), resumo);
        } finally {
            MDC.remove(LoggingContextKeys.UPLOAD_ID);
        }
    }

    private ArquivoProcessamento buscarArquivo(UUID id) {
        return arquivoProcessamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo nao encontrado"));
    }
}
