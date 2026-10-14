package com.notebookcontrol.api.model;

public class MovimentoRequest {
    private String notebookId;
    private String nfcTag;
    private String pessoa;

    public String getNotebookId() { return notebookId; }
    public void setNotebookId(String notebookId) { this.notebookId = notebookId; }

    public String getNfcTag() { return nfcTag; }
    public void setNfcTag(String nfcTag) { this.nfcTag = nfcTag; }

    public String getPessoa() { return pessoa; }
    public void setPessoa(String pessoa) { this.pessoa = pessoa; }
}
