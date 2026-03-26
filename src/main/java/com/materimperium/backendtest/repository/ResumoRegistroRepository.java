package com.materimperium.backendtest.repository;

import com.materimperium.backendtest.domain.ResumoRegistro;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumoRegistroRepository extends JpaRepository<ResumoRegistro, Long> {

    List<ResumoRegistro> findAllByArquivoProcessamentoIdOrderByRegistroAsc(UUID arquivoId);
}
