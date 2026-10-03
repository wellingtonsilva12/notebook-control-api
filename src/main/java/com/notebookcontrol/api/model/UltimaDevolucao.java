package com.notebookcontrol.api.model;

public class UltimaDevolucao {
    private String pessoa;
    private Long data;

    public UltimaDevolucao() {}

    public UltimaDevolucao(String pessoa, Long data) {
        this.pessoa = pessoa;
        this.data = data;
    }

    public String getPessoa() { return pessoa; }
    public void setPessoa(String pessoa) { this.pessoa = pessoa; }

    public Long getData() { return data; }
    public void setData(Long data) { this.data = data; }
}
