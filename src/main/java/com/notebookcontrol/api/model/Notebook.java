package com.notebookcontrol.api.model;

public class Notebook {
    private String id;
    private String nome;
    private String patrimonio;
    private String nfcTag;
    private boolean bioRegistrada;
    private String status; // "disponivel" | "retirado"
    private String retiradoPor;
    private Long dataRetirada;
    private UltimaDevolucao ultimaDevolucao;
    private Long criadoEm;

    public Notebook() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getPatrimonio() { return patrimonio; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }

    public String getNfcTag() { return nfcTag; }
    public void setNfcTag(String nfcTag) { this.nfcTag = nfcTag; }

    public boolean isBioRegistrada() { return bioRegistrada; }
    public void setBioRegistrada(boolean bioRegistrada) { this.bioRegistrada = bioRegistrada; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRetiradoPor() { return retiradoPor; }
    public void setRetiradoPor(String retiradoPor) { this.retiradoPor = retiradoPor; }

    public Long getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(Long dataRetirada) { this.dataRetirada = dataRetirada; }

    public UltimaDevolucao getUltimaDevolucao() { return ultimaDevolucao; }
    public void setUltimaDevolucao(UltimaDevolucao ultimaDevolucao) { this.ultimaDevolucao = ultimaDevolucao; }

    public Long getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Long criadoEm) { this.criadoEm = criadoEm; }
}
