package com.materimperium.backendtest.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "arquivo_processamento")
public class ArquivoProcessamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusProcessamento status;

    @Column(length = 2000)
    private String mensagemErro;

    @Column(nullable = false)
    private Instant criadoEm;

    private Instant iniciadoEm;

    private Instant finalizadoEm;

    @Column(nullable = false)
    private String caminhoTemporario;

    @OneToMany(mappedBy = "arquivoProcessamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ResumoRegistro> resumos = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public StatusProcessamento getStatus() {
        return status;
    }

    public void setStatus(StatusProcessamento status) {
        this.status = status;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(Instant iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public void setFinalizadoEm(Instant finalizadoEm) {
        this.finalizadoEm = finalizadoEm;
    }

    public String getCaminhoTemporario() {
        return caminhoTemporario;
    }

    public void setCaminhoTemporario(String caminhoTemporario) {
        this.caminhoTemporario = caminhoTemporario;
    }

    public List<ResumoRegistro> getResumos() {
        return resumos;
    }
}
