package com.notebookcontrol.api.controller;

import com.notebookcontrol.api.model.*;
import com.notebookcontrol.api.service.AdminService;
import com.notebookcontrol.api.service.NotebookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotebookController {

    private final NotebookService service;
    private final AdminService admin;

    public NotebookController(NotebookService service, AdminService admin) {
        this.service = service;
        this.admin = admin;
    }

    // ---------- Administrador ----------

    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        String token = admin.login(body.get("senha"), ip);
        return Map.of("token", token);
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

    @PutMapping("/notebooks/{id}")
    public Notebook updateNotebook(@PathVariable String id, @RequestBody NotebookRequest req,
                                   @RequestHeader(value = "x-admin-token", required = false) String token) {
        admin.exigirAdmin(token);
        return service.updateNotebook(id, req.getNome(), req.getPatrimonio());
    }

    @DeleteMapping("/notebooks/{id}")
    public Map<String, Object> deleteNotebook(@PathVariable String id,
                                              @RequestHeader(value = "x-admin-token", required = false) String token) {
        admin.exigirAdmin(token);
        service.deleteNotebook(id);
        return Map.of("ok", true);
    }

    @PostMapping("/notebooks/{id}/liberar")
    public Map<String, Object> liberarNotebook(@PathVariable String id,
                                               @RequestHeader(value = "x-admin-token", required = false) String token) {
        admin.exigirAdmin(token);
        return service.liberarNotebook(id);
    }

    // ---------- Movimentos ----------

    @GetMapping("/movimentos")
    public List<Movimento> listMovimentos() {
        return service.listMovimentos();
    }

    @DeleteMapping("/movimentos")
    public Map<String, Object> limparMovimentos(@RequestHeader(value = "x-admin-token", required = false) String token) {
        admin.exigirAdmin(token);
        service.limparMovimentos();
        return Map.of("ok", true);
    }

    @PostMapping("/movimentos")
    public ResponseEntity<Map<String, Object>> registrarMovimento(@RequestBody MovimentoRequest req) {
        Map<String, Object> result = service.registrarMovimento(req.getNotebookId(), req.getNfcTag(), req.getPessoa());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
