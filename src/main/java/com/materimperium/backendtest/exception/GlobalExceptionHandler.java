package com.materimperium.backendtest.exception;

import com.materimperium.backendtest.dto.ErroResponse;
import com.materimperium.backendtest.dto.StatusResponse;
import com.materimperium.backendtest.domain.StatusProcessamento;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleArquivoInvalido(ArquivoInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(ArmazenamentoArquivoException.class)
    public ResponseEntity<ErroResponse> handleArmazenamento(ArmazenamentoArquivoException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(ProcessamentoEmAndamentoException.class)
    public ResponseEntity<StatusResponse> handleProcessando(ProcessamentoEmAndamentoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new StatusResponse((UUID) null, StatusProcessamento.EM_PROCESSAMENTO));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse("Requisicao invalida"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> handleParteObrigatoriaAusente(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse("Arquivo nao enviado"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse("Parametro invalido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Erro interno do servidor"));
    }
}
