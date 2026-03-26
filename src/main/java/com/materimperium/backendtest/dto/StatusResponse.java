package com.materimperium.backendtest.dto;

import com.materimperium.backendtest.domain.StatusProcessamento;
import java.util.UUID;

public record StatusResponse(UUID id, StatusProcessamento status) {
}
