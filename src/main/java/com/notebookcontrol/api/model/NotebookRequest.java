package com.notebookcontrol.api.model;

public class NotebookRequest {
    private String nome;
    private String patrimonio;
    private String nfcTag;
    private boolean bioRegistrada;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getPatrimonio() { return patrimonio; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }

    public String getNfcTag() { return nfcTag; }
    public void setNfcTag(String nfcTag) { this.nfcTag = nfcTag; }

    public boolean isBioRegistrada() { return bioRegistrada; }
    public void setBioRegistrada(boolean bioRegistrada) { this.bioRegistrada = bioRegistrada; }
}
