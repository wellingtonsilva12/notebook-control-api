package com.notebookcontrol.api.model;

public class Movimento {
    private String id;
    private String notebookId;
    private String notebookNome;
    private String tipo; // "saida" | "entrada"
    private String pessoa;
    private String nfcTag;
    private Long timestamp;

    public Movimento() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNotebookId() { return notebookId; }
    public void setNotebookId(String notebookId) { this.notebookId = notebookId; }

    public String getNotebookNome() { return notebookNome; }
    public void setNotebookNome(String notebookNome) { this.notebookNome = notebookNome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getPessoa() { return pessoa; }
    public void setPessoa(String pessoa) { this.pessoa = pessoa; }

    public String getNfcTag() { return nfcTag; }
    public void setNfcTag(String nfcTag) { this.nfcTag = nfcTag; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
