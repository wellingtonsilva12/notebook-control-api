package com.notebookcontrol.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notebookcontrol.api.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotebookService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path dataDir = Path.of(System.getProperty("user.dir"), "data");
    private final Path dataFile = dataDir.resolve("db.json");
    private final Object lock = new Object();

    public NotebookService() {
        try {
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
            if (!Files.exists(dataFile)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), new Database());
            }
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível preparar o armazenamento de dados.", e);
        }
    }

    private Database readDb() {
        synchronized (lock) {
            try {
                return mapper.readValue(dataFile.toFile(), Database.class);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao ler os dados.", e);
            }
        }
    }

    private void writeDb(Database db) {
        synchronized (lock) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), db);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao salvar os dados.", e);
            }
        }
    }

    private String uid() {
        return Long.toString(System.currentTimeMillis(), 36) + UUID.randomUUID().toString().substring(0, 6);
    }

    // ---------- Notebooks ----------

    public List<Notebook> listNotebooks() {
        return readDb().getNotebooks();
    }

    public Notebook createNotebook(String nome, String patrimonio, String nfcTag, boolean bioRegistrada) {
        if (nome == null || nome.isBlank() || nfcTag == null || nfcTag.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Os campos \"nome\" e \"nfcTag\" são obrigatórios.");
        }
        synchronized (lock) {
            Database db = readDb();
            boolean tagJaUsada = db.getNotebooks().stream().anyMatch(n -> nfcTag.equals(n.getNfcTag()));
            if (tagJaUsada) {
                throw new ApiException(HttpStatus.CONFLICT, "Já existe um notebook cadastrado com essa tag NFC.");
            }
            Notebook notebook = new Notebook();
            notebook.setId(uid());
            notebook.setNome(nome);
            notebook.setPatrimonio(patrimonio == null ? "" : patrimonio);
            notebook.setNfcTag(nfcTag);
            notebook.setBioRegistrada(bioRegistrada);
            notebook.setStatus("disponivel");
            notebook.setRetiradoPor(null);
            notebook.setDataRetirada(null);
            notebook.setUltimaDevolucao(null);
            notebook.setCriadoEm(System.currentTimeMillis());

            db.getNotebooks().add(notebook);
            writeDb(db);
            return notebook;
        }
    }

    public Notebook findByTag(String tag) {
        return readDb().getNotebooks().stream()
                .filter(n -> tag.equals(n.getNfcTag()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nenhum notebook encontrado com essa tag."));
    }

    public void deleteNotebook(String id) {
        synchronized (lock) {
            Database db = readDb();
            boolean removed = db.getNotebooks().removeIf(n -> id.equals(n.getId()));
            if (!removed) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Notebook não encontrado.");
            }
            writeDb(db);
        }
    }

    // ---------- Movimentos ----------

    public List<Movimento> listMovimentos() {
        return readDb().getMovimentos();
    }

    public Map<String, Object> registrarMovimento(String notebookId, String nfcTag, String pessoa) {
        if (pessoa == null || pessoa.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O campo \"pessoa\" é obrigatório.");
        }
        synchronized (lock) {
            Database db = readDb();
            Notebook nb = db.getNotebooks().stream()
                    .filter(n -> (notebookId != null && notebookId.equals(n.getId()))
                              || (nfcTag != null && nfcTag.equals(n.getNfcTag())))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notebook não encontrado (verifique id/tag)."));

            String tipo = "disponivel".equals(nb.getStatus()) ? "saida" : "entrada";
            long now = System.currentTimeMillis();

            if (tipo.equals("saida")) {
                nb.setStatus("retirado");
                nb.setRetiradoPor(pessoa);
                nb.setDataRetirada(now);
            } else {
                nb.setStatus("disponivel");
                nb.setRetiradoPor(null);
                nb.setDataRetirada(null);
                nb.setUltimaDevolucao(new UltimaDevolucao(pessoa, now));
            }

            Movimento movimento = new Movimento();
            movimento.setId(uid());
            movimento.setNotebookId(nb.getId());
            movimento.setNotebookNome(nb.getNome());
            movimento.setTipo(tipo);
            movimento.setPessoa(pessoa);
            movimento.setNfcTag(nb.getNfcTag());
            movimento.setTimestamp(now);

            db.getMovimentos().add(movimento);
            writeDb(db);

            return Map.of("notebook", nb, "movimento", movimento);
        }
    }
}
