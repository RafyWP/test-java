package com.materimperium.backendtest.repository;

import com.materimperium.backendtest.domain.ArquivoProcessamento;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArquivoProcessamentoRepository extends JpaRepository<ArquivoProcessamento, UUID> {
}
