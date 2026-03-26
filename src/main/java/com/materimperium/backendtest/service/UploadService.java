package com.materimperium.backendtest.service;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import com.materimperium.backendtest.domain.StatusProcessamento;
import com.materimperium.backendtest.exception.ArmazenamentoArquivoException;
import com.materimperium.backendtest.exception.ArquivoInvalidoException;
import com.materimperium.backendtest.logging.LoggingContextKeys;
import com.materimperium.backendtest.repository.ArquivoProcessamentoRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final ArquivoProcessamentoRepository arquivoProcessamentoRepository;
    private final ProcessamentoAsyncService processamentoAsyncService;
    private final Path diretorioTemporario;

    public UploadService(
            ArquivoProcessamentoRepository arquivoProcessamentoRepository,
            ProcessamentoAsyncService processamentoAsyncService,
            @Value("${app.storage.temp-dir:/tmp/materimperium}") String diretorioTemporario
    ) {
        this.arquivoProcessamentoRepository = arquivoProcessamentoRepository;
        this.processamentoAsyncService = processamentoAsyncService;
        this.diretorioTemporario = Path.of(diretorioTemporario);
    }

    public UUID receberArquivo(MultipartFile file) {
        log.info("Recebendo upload de arquivo nome={} tamanho={} bytes", file.getOriginalFilename(), file.getSize());
        validarArquivo(file);

        try {
            Files.createDirectories(diretorioTemporario);
            Path arquivoTemporario = Files.createTempFile(diretorioTemporario, "upload-", ".txt");
            Files.copy(file.getInputStream(), arquivoTemporario, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            ArquivoProcessamento arquivo = new ArquivoProcessamento();
            arquivo.setNomeArquivo(file.getOriginalFilename() != null ? file.getOriginalFilename() : "arquivo.txt");
            arquivo.setStatus(StatusProcessamento.EM_PROCESSAMENTO);
            arquivo.setCriadoEm(Instant.now());
            arquivo.setCaminhoTemporario(arquivoTemporario.toString());

            ArquivoProcessamento salvo = arquivoProcessamentoRepository.save(arquivo);
            String correlationId = MDC.get(LoggingContextKeys.CORRELATION_ID);
            log.info("Upload aceito para processamento uploadId={} arquivo={}", salvo.getId(), salvo.getNomeArquivo());
            processamentoAsyncService.processarArquivo(salvo.getId(), correlationId);
            return salvo.getId();
        } catch (IOException ex) {
            log.error("Falha ao armazenar arquivo temporario para processamento", ex);
            throw new ArmazenamentoArquivoException("Nao foi possivel armazenar o arquivo para processamento");
        }
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("Upload rejeitado: arquivo vazio");
            throw new ArquivoInvalidoException("Arquivo vazio");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String primeiraLinha = reader.readLine();
            String segundaLinha = reader.readLine();

            if (primeiraLinha == null || segundaLinha == null) {
                log.warn("Upload rejeitado: arquivo com menos de duas linhas");
                throw new ArquivoInvalidoException("Arquivo deve conter ao menos duas linhas");
            }

            boolean cabecalhoValido = primeiraLinha.startsWith("|0000|017|") || primeiraLinha.startsWith("|0000|006|");
            if (!cabecalhoValido) {
                log.warn("Upload rejeitado: primeira linha invalida");
                throw new ArquivoInvalidoException("Primeira linha invalida");
            }

            if (!"|0001|0|".equals(segundaLinha)) {
                log.warn("Upload rejeitado: segunda linha invalida");
                throw new ArquivoInvalidoException("Segunda linha invalida");
            }
        } catch (IOException ex) {
            log.error("Falha ao ler arquivo enviado", ex);
            throw new ArquivoInvalidoException("Nao foi possivel ler o arquivo enviado");
        }
    }
}
