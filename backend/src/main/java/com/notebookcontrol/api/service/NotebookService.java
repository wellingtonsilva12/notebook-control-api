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

    private final HorarioService horario;

    public NotebookService(HorarioService horario) {
        this.horario = horario;
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

    public Notebook updateNotebook(String id, String nome, String patrimonio) {
        if (nome == null || nome.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O campo \"nome\" é obrigatório.");
        }
        synchronized (lock) {
            Database db = readDb();
            Notebook nb = db.getNotebooks().stream()
                    .filter(n -> id.equals(n.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notebook não encontrado."));
            nb.setNome(nome);
            nb.setPatrimonio(patrimonio == null ? "" : patrimonio);
            writeDb(db);
            return nb;
        }
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

    // ---------- Ações do administrador ----------

    public void limparMovimentos() {
        synchronized (lock) {
            Database db = readDb();
            db.getMovimentos().clear();
            writeDb(db);
        }
    }

    // ---------- Movimentos ----------

    public List<Movimento> listMovimentos() {
        return readDb().getMovimentos();
    }

    public Map<String, Object> registrarMovimento(String notebookId, String nfcTag, String pessoa, boolean admin) {
        synchronized (lock) {
            Database db = readDb();
            Notebook nb = db.getNotebooks().stream()
                    .filter(n -> (notebookId != null && notebookId.equals(n.getId()))
                              || (nfcTag != null && nfcTag.equals(n.getNfcTag())))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notebook não encontrado (verifique id/tag)."));

            String status = nb.getStatus();
            long now = System.currentTimeMillis();
            String tipo;
            String quem;

            if ("pendente".equals(status)) {
                throw new ApiException(HttpStatus.CONFLICT, "A devolução deste notebook já foi registrada e aguarda confirmação do responsável.");
            } else if ("disponivel".equals(status)) {
                // SAÍDA: só nos primeiros 30 min da aula (o administrador pode registrar fora dela)
                if (!admin && !horario.retiradaAberta()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, horario.mensagemRetirada());
                }
                // precisa do nome de quem está retirando
                if (pessoa == null || pessoa.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "O campo \"pessoa\" é obrigatório.");
                }
                tipo = "saida";
                quem = pessoa.trim();
                nb.setStatus("retirado");
                nb.setRetiradoPor(quem);
                nb.setDataRetirada(now);
                nb.setDevolucaoSolicitadaEm(null);
            } else {
                // DEVOLUÇÃO: no meio da aula só o administrador; depois deixa PENDENTE até o responsável confirmar
                if (!admin && !horario.devolucaoAberta()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, horario.mensagemDevolucao());
                }
                tipo = "pendente";
                quem = nb.getRetiradoPor();
                nb.setStatus("pendente");
                nb.setDevolucaoSolicitadaEm(now);
            }

            Movimento movimento = new Movimento();
            movimento.setId(uid());
            movimento.setNotebookId(nb.getId());
            movimento.setNotebookNome(nb.getNome());
            movimento.setTipo(tipo);
            movimento.setPessoa(quem);
            movimento.setNfcTag(nb.getNfcTag());
            movimento.setTimestamp(now);

            db.getMovimentos().add(movimento);
            writeDb(db);

            return Map.of("notebook", nb, "movimento", movimento);
        }
    }

    /**
     * Admin confirma a devolução.
     * - Notebook PENDENTE (a pessoa registrou a devolução): comentário opcional.
     * - Notebook RETIRADO (ex.: aluno saiu mais cedo, esqueceu de devolver): comentário obrigatório,
     *   a devolução é registrada como "Admin forçado".
     */
    public Map<String, Object> confirmarDevolucao(String id, String comentario) {
        String obs = comentario == null ? "" : comentario.trim();
        if (obs.length() > 300) obs = obs.substring(0, 300);
        synchronized (lock) {
            Database db = readDb();
            Notebook nb = db.getNotebooks().stream()
                    .filter(n -> id.equals(n.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notebook não encontrado."));
            if ("disponivel".equals(nb.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "Este notebook já está disponível.");
            }
            boolean pendente = "pendente".equals(nb.getStatus());
            if (!pendente && obs.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um comentário para confirmar a devolução.");
            }
            long now = System.currentTimeMillis();
            String quem = pendente ? nb.getRetiradoPor() : "Admin forçado";
            if (pendente && obs.isEmpty()) obs = "Devolução confirmada pelo responsável";

            nb.setStatus("disponivel");
            nb.setRetiradoPor(null);
            nb.setDataRetirada(null);
            nb.setDevolucaoSolicitadaEm(null);
            nb.setUltimaDevolucao(new UltimaDevolucao(quem, now));

            Movimento movimento = new Movimento();
            movimento.setId(uid());
            movimento.setNotebookId(nb.getId());
            movimento.setNotebookNome(nb.getNome());
            movimento.setTipo("entrada");
            movimento.setPessoa(quem);
            movimento.setNfcTag(nb.getNfcTag());
            movimento.setComentario(obs);
            movimento.setTimestamp(now);
            db.getMovimentos().add(movimento);
            writeDb(db);
            return Map.of("notebook", nb, "movimento", movimento);
        }
    }

    /**
     * Admin encerra a aula: devolve os notebooks que ainda constam como RETIRADOS. Os pendentes (devolução já registrada) ficam como estão, para confirmação individual.
     * Comentário obrigatório. Cada devolução vira uma "entrada" feita por "Admin forçado".
     */
    public Map<String, Object> devolverTodos(String comentario) {
        String obs = comentario == null ? "" : comentario.trim();
        if (obs.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um comentário para devolver todos (ex.: aula encerrada mais cedo).");
        }
        if (obs.length() > 300) obs = obs.substring(0, 300);
        synchronized (lock) {
            Database db = readDb();
            long now = System.currentTimeMillis();
            List<Movimento> novos = new java.util.ArrayList<>();
            for (Notebook nb : db.getNotebooks()) {
                if (!"retirado".equals(nb.getStatus())) continue;
                nb.setStatus("disponivel");
                nb.setRetiradoPor(null);
                nb.setDataRetirada(null);
                nb.setDevolucaoSolicitadaEm(null);
                nb.setUltimaDevolucao(new UltimaDevolucao("Admin forçado", now));

                Movimento mv = new Movimento();
                mv.setId(uid());
                mv.setNotebookId(nb.getId());
                mv.setNotebookNome(nb.getNome());
                mv.setTipo("entrada");
                mv.setPessoa("Admin forçado");
                mv.setNfcTag(nb.getNfcTag());
                mv.setComentario(obs);
                mv.setTimestamp(now);
                novos.add(mv);
            }
            if (novos.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "Não há notebooks retirados para devolver.");
            }
            db.getMovimentos().addAll(novos);
            writeDb(db);
            return Map.of("quantidade", novos.size(), "notebooks", db.getNotebooks(), "movimentos", novos);
        }
    }
}
