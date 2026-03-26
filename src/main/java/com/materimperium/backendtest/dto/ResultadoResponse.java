package com.materimperium.backendtest.dto;

import com.materimperium.backendtest.domain.StatusProcessamento;
import java.util.List;

public record ResultadoResponse(StatusProcessamento status, List<ResumoItemResponse> resumo) {
}
