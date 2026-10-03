package com.notebookcontrol.api.controller;

import com.notebookcontrol.api.model.*;
import com.notebookcontrol.api.service.NotebookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotebookController {

    private final NotebookService service;

    public NotebookController(NotebookService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "notebook-control-api-spring");
    }

    // ---------- Notebooks ----------

    @GetMapping("/notebooks")
    public List<Notebook> listNotebooks() {
        return service.listNotebooks();
    }

    @PostMapping("/notebooks")
    public ResponseEntity<Notebook> createNotebook(@RequestBody NotebookRequest req) {
        Notebook created = service.createNotebook(req.getNome(), req.getPatrimonio(), req.getNfcTag(), req.isBioRegistrada());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/notebooks/by-tag/{tag}")
    public Notebook getByTag(@PathVariable String tag) {
        return service.findByTag(tag);
    }

    @DeleteMapping("/notebooks/{id}")
    public Map<String, Object> deleteNotebook(@PathVariable String id) {
        service.deleteNotebook(id);
        return Map.of("ok", true);
    }

    // ---------- Movimentos ----------

    @GetMapping("/movimentos")
    public List<Movimento> listMovimentos() {
        return service.listMovimentos();
    }

    @PostMapping("/movimentos")
    public ResponseEntity<Map<String, Object>> registrarMovimento(@RequestBody MovimentoRequest req) {
        Map<String, Object> result = service.registrarMovimento(req.getNotebookId(), req.getNfcTag(), req.getPessoa());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
