package com.materimperium.backendtest.controller;

import com.materimperium.backendtest.dto.ResultadoResponse;
import com.materimperium.backendtest.dto.StatusResponse;
import com.materimperium.backendtest.dto.UploadResponse;
import com.materimperium.backendtest.dto.ErroResponse;
import com.materimperium.backendtest.service.ConsultaService;
import com.materimperium.backendtest.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "Endpoints para upload, consulta de status e consulta de resultado.")
public class UploadController {

    private final UploadService uploadService;
    private final ConsultaService consultaService;

    public UploadController(UploadService uploadService, ConsultaService consultaService) {
        this.uploadService = uploadService;
        this.consultaService = consultaService;
    }

    @PostMapping
    @Operation(
            summary = "Recebe um arquivo para processamento",
            description = "Valida as duas primeiras linhas do arquivo, cria um identificador unico e inicia o processamento em background.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Upload aceito para processamento",
                    content = @Content(schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo invalido ou ausente",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para enviar arquivo")
    })
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID id = uploadService.receberArquivo(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new UploadResponse(id));
    }

    @GetMapping("/{id}/status")
    @Operation(
            summary = "Consulta o status do processamento",
            description = "Retorna o status atual do upload informado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status encontrado",
                    content = @Content(schema = @Schema(implementation = StatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para consultar status"),
            @ApiResponse(responseCode = "404", description = "Upload nao encontrado")
    })
    public StatusResponse consultarStatus(@PathVariable UUID id) {
        return consultaService.consultarStatus(id);
    }

    @GetMapping("/{id}/resultado")
    @Operation(
            summary = "Consulta o resultado final do processamento",
            description = "Retorna o status final e o resumo por codigo de registro. Se ainda estiver em andamento, retorna conflito com status EM_PROCESSAMENTO.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado encontrado",
                    content = @Content(schema = @Schema(implementation = ResultadoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao para consultar resultado"),
            @ApiResponse(responseCode = "404", description = "Upload nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Processamento em andamento",
                    content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    })
    public ResultadoResponse consultarResultado(@PathVariable UUID id) {
        return consultaService.consultarResultado(id);
    }
}
