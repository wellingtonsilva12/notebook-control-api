package com.notebookcontrol.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private List<Notebook> notebooks = new ArrayList<>();
    private List<Movimento> movimentos = new ArrayList<>();
    // LEGADO: PINs de versões antigas. Não é mais usado; mantido só para o db.json antigo continuar abrindo.
    private Map<String, String> pins = new HashMap<>();

    public List<Notebook> getNotebooks() { return notebooks; }
    public void setNotebooks(List<Notebook> notebooks) { this.notebooks = notebooks; }

    public Map<String, String> getPins() { return pins; }
    public void setPins(Map<String, String> pins) { this.pins = pins; }

    public List<Movimento> getMovimentos() { return movimentos; }
    public void setMovimentos(List<Movimento> movimentos) { this.movimentos = movimentos; }
}
