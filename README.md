# API de Controle de Notebooks

Backend simples (Node.js + Express) que guarda os notebooks cadastrados e o
histórico de saídas/devoluções em um arquivo JSON (`data/db.json`). É o que
faz o app web (o HTML publicado) funcionar igual em qualquer aparelho —
celular, computador, etc. — em vez de guardar tudo só no navegador.

## Rodando localmente

Requer [Node.js](https://nodejs.org) 18 ou mais novo instalado.

```bash
npm install
npm start
```

O servidor sobe em `http://localhost:3000`. Endpoints disponíveis:

- `GET  /api/health` — verifica se está no ar
- `GET  /api/notebooks` — lista os notebooks
- `POST /api/notebooks` — cadastra um notebook `{ nome, patrimonio, nfcTag, bioRegistrada }`
- `GET  /api/notebooks/by-tag/:tag` — busca um notebook pela tag NFC
- `DELETE /api/notebooks/:id` — remove um notebook
- `GET  /api/movimentos` — lista o histórico
- `POST /api/movimentos` — registra saída ou devolução `{ notebookId ou nfcTag, pessoa }`
  (o servidor decide sozinho se é saída ou devolução, olhando o status atual do notebook)

## Segurança (opcional, mas recomendado)

Por padrão a API fica **aberta** para quem tiver a URL. Se for expor na
internet, defina uma chave antes de rodar:

```bash
API_KEY=uma-senha-bem-longa npm start
```

Com isso, toda requisição precisa do header `x-api-key: uma-senha-bem-longa`.
No app web, cole essa mesma chave no campo "Chave de API" da tela de
Configurações.

## Como colocar essa API no ar (escolha uma opção)

### Opção 1 — Serviço gratuito na nuvem (mais simples para acessar de qualquer lugar)
Suba esta pasta para um repositório no GitHub e conecte em um serviço como
[Render](https://render.com) ou [Railway](https://railway.app):
- Build command: `npm install`
- Start command: `npm start`
- Variável de ambiente opcional: `API_KEY`

Ao terminar o deploy, você recebe uma URL pública tipo
`https://seu-app.onrender.com` — é essa URL que vai no campo "URL da API"
do app.

### Opção 2 — Computador/servidor na sua rede local
Se o app só precisa funcionar dentro da sua rede Wi-Fi (empresa/casa):

```bash
npm install
npm start
```

Descubra o IP local da máquina (`ipconfig` no Windows, `ifconfig` ou `ip a`
no Linux/Mac) e use `http://SEU-IP-LOCAL:3000` como URL da API — funciona
para qualquer celular/computador conectado na mesma rede.

### Opção 3 — Raspberry Pi / mini-servidor dedicado
Mesmo processo da Opção 2, deixando o Raspberry Pi ligado 24h. Para manter o
servidor rodando mesmo se cair, use o [PM2](https://pm2.keymetrics.io/):

```bash
npm install -g pm2
pm2 start server.js --name notebook-api
pm2 save
pm2 startup
```

## Backup dos dados

Todos os dados ficam em `data/db.json`. Basta copiar esse arquivo para fazer
backup ou migrar de servidor.
