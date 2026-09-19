const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.API_KEY || null; // opcional — defina para exigir header x-api-key

const DATA_DIR = path.join(__dirname, 'data');
const DATA_FILE = path.join(DATA_DIR, 'db.json');

if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
if (!fs.existsSync(DATA_FILE)) {
  fs.writeFileSync(DATA_FILE, JSON.stringify({ notebooks: [], movimentos: [] }, null, 2));
}

function readDB() {
  return JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
}
function writeDB(db) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2));
}
function uid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 9);
}

app.use(cors());
app.use(express.json());

// Autenticação simples opcional por chave de API
if (API_KEY) {
  app.use((req, res, next) => {
    if (req.path === '/api/health') return next();
    if (req.headers['x-api-key'] !== API_KEY) {
      return res.status(401).json({ error: 'Chave de API inválida ou ausente.' });
    }
    next();
  });
}

app.get('/api/health', (req, res) => {
  res.json({ ok: true, service: 'notebook-control-api' });
});

// ---------- Notebooks ----------

app.get('/api/notebooks', (req, res) => {
  const db = readDB();
  res.json(db.notebooks);
});

app.post('/api/notebooks', (req, res) => {
  const { nome, patrimonio, nfcTag, bioRegistrada } = req.body || {};
  if (!nome || !nfcTag) {
    return res.status(400).json({ error: 'Os campos "nome" e "nfcTag" são obrigatórios.' });
  }
  const db = readDB();
  if (db.notebooks.some((n) => n.nfcTag === nfcTag)) {
    return res.status(409).json({ error: 'Já existe um notebook cadastrado com essa tag NFC.' });
  }
  const notebook = {
    id: uid(),
    nome,
    patrimonio: patrimonio || '',
    nfcTag,
    bioRegistrada: !!bioRegistrada,
    status: 'disponivel',
    retiradoPor: null,
    dataRetirada: null,
    ultimaDevolucao: null,
    criadoEm: Date.now(),
  };
  db.notebooks.push(notebook);
  writeDB(db);
  res.status(201).json(notebook);
});

app.get('/api/notebooks/by-tag/:tag', (req, res) => {
  const db = readDB();
  const nb = db.notebooks.find((n) => n.nfcTag === req.params.tag);
  if (!nb) return res.status(404).json({ error: 'Nenhum notebook encontrado com essa tag.' });
  res.json(nb);
});

app.delete('/api/notebooks/:id', (req, res) => {
  const db = readDB();
  const before = db.notebooks.length;
  db.notebooks = db.notebooks.filter((n) => n.id !== req.params.id);
  if (db.notebooks.length === before) {
    return res.status(404).json({ error: 'Notebook não encontrado.' });
  }
  writeDB(db);
  res.json({ ok: true });
});

// ---------- Movimentações (saída / devolução) ----------

app.get('/api/movimentos', (req, res) => {
  const db = readDB();
  res.json(db.movimentos);
});

app.post('/api/movimentos', (req, res) => {
  const { notebookId, nfcTag, pessoa } = req.body || {};
  if (!pessoa) return res.status(400).json({ error: 'O campo "pessoa" é obrigatório.' });

  const db = readDB();
  const nb = db.notebooks.find((n) => n.id === notebookId || n.nfcTag === nfcTag);
  if (!nb) return res.status(404).json({ error: 'Notebook não encontrado (verifique id/tag).' });

  const tipo = nb.status === 'disponivel' ? 'saida' : 'entrada';
  const now = Date.now();

  if (tipo === 'saida') {
    nb.status = 'retirado';
    nb.retiradoPor = pessoa;
    nb.dataRetirada = now;
  } else {
    nb.status = 'disponivel';
    nb.retiradoPor = null;
    nb.dataRetirada = null;
    nb.ultimaDevolucao = { pessoa, data: now };
  }

  const movimento = {
    id: uid(),
    notebookId: nb.id,
    notebookNome: nb.nome,
    tipo,
    pessoa,
    nfcTag: nb.nfcTag,
    timestamp: now,
  };

  db.movimentos.push(movimento);
  writeDB(db);
  res.status(201).json({ notebook: nb, movimento });
});

app.listen(PORT, () => {
  console.log(`API de controle de notebooks rodando em http://localhost:${PORT}`);
  if (API_KEY) console.log('Autenticação por x-api-key está ATIVA.');
  else console.log('Nenhuma API_KEY definida — a API está aberta para quem tiver a URL.');
});
