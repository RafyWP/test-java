package com.materimperium.backendtest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resumo_registro")
public class ResumoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arquivo_id", nullable = false)
    private ArquivoProcessamento arquivoProcessamento;

    @Column(nullable = false, length = 20)
    private String registro;

    @Column(nullable = false)
    private Long total;

    public Long getId() {
        return id;
    }

    public ArquivoProcessamento getArquivoProcessamento() {
        return arquivoProcessamento;
    }

    public void setArquivoProcessamento(ArquivoProcessamento arquivoProcessamento) {
        this.arquivoProcessamento = arquivoProcessamento;
    }

    public String getRegistro() {
        return registro;
    }

    public void setRegistro(String registro) {
        this.registro = registro;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
