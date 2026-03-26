package com.materimperium.backendtest.controller;

import com.materimperium.backendtest.dto.ResultadoResponse;
import com.materimperium.backendtest.dto.StatusResponse;
import com.materimperium.backendtest.dto.UploadResponse;
import com.materimperium.backendtest.service.ConsultaService;
import com.materimperium.backendtest.service.UploadService;
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
public class UploadController {

    private final UploadService uploadService;
    private final ConsultaService consultaService;

    public UploadController(UploadService uploadService, ConsultaService consultaService) {
        this.uploadService = uploadService;
        this.consultaService = consultaService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID id = uploadService.receberArquivo(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new UploadResponse(id));
    }

    @GetMapping("/{id}/status")
    public StatusResponse consultarStatus(@PathVariable UUID id) {
        return consultaService.consultarStatus(id);
    }

    @GetMapping("/{id}/resultado")
    public ResultadoResponse consultarResultado(@PathVariable UUID id) {
        return consultaService.consultarResultado(id);
    }
}
