package com.notebookcontrol.api.model;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private List<Notebook> notebooks = new ArrayList<>();
    private List<Movimento> movimentos = new ArrayList<>();

    public List<Notebook> getNotebooks() { return notebooks; }
    public void setNotebooks(List<Notebook> notebooks) { this.notebooks = notebooks; }

    public List<Movimento> getMovimentos() { return movimentos; }
    public void setMovimentos(List<Movimento> movimentos) { this.movimentos = movimentos; }
}
