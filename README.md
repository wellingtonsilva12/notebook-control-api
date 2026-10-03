# API de Controle de Notebooks — versão Spring Boot

Mesma API de antes (notebooks + histórico de saídas/devoluções), só que reescrita
em **Java 17 + Spring Boot 3**, em vez de Node.js. Os endpoints são idênticos,
então o app web (HTML) não precisa de nenhuma alteração — é só trocar a URL da
API nas Configurações.

## Rodando localmente

Requer [Java 17+](https://adoptium.net) e [Maven](https://maven.apache.org).

```bash
mvn spring-boot:run
```

O servidor sobe em `http://localhost:3000` (ou na porta definida na variável
de ambiente `PORT`).

## Endpoints

- `GET  /api/health`
- `GET  /api/notebooks`
- `POST /api/notebooks` — `{ nome, patrimonio, nfcTag, bioRegistrada }`
- `GET  /api/notebooks/by-tag/{tag}`
- `DELETE /api/notebooks/{id}`
- `GET  /api/movimentos`
- `POST /api/movimentos` — `{ notebookId ou nfcTag, pessoa }`

## Segurança (opcional)

Defina a variável de ambiente `API_KEY` para exigir o header `x-api-key` em
toda requisição (exceto `/api/health`). Sem essa variável, a API fica aberta
para quem tiver a URL — igual à versão em Node.

## Deploy no Render

Diferente do Node, o Render **não roda Java nativamente** — precisa ser via
**Docker**, que já está pronto neste projeto (`Dockerfile` na raiz).

1. Suba esta pasta inteira (`pom.xml`, `Dockerfile`, pasta `src`) para um
   repositório no GitHub.
2. No Render, crie um novo **Web Service** apontando pra esse repositório.
3. Na etapa de configuração, em **"Language"**, troque de "Node"/"Auto" para
   **"Docker"**. O Render detecta o `Dockerfile` sozinho — deixe os campos de
   Build Command e Start Command em branco.
4. Plano: **Free**.
5. (Opcional) Em "Environment Variables", adicione `API_KEY` se quiser exigir
   autenticação.
6. Deploy.

## ⚠️ Sobre a persistência dos dados

Os dados ficam salvos em `data/db.json`, dentro do próprio contêiner. No
plano gratuito do Render, **esse disco não é permanente**: toda vez que o
serviço reinicia (um novo deploy, por exemplo), os dados cadastrados são
perdidos. Isso vale tanto para essa versão em Spring Boot quanto para a
versão em Node — é uma limitação do plano gratuito, não do código.

Se isso for um problema no seu uso real (querer manter o histórico por muito
tempo), o próximo passo seria usar um banco de dados gerenciado — por exemplo
um Render Postgres gratuito — em vez de um arquivo local. Posso te ajudar a
adaptar o projeto para isso quando quiser.
