# Plano de Execução Restante – MPet PetMatch

Data: 2025-12-02

Este documento lista o que falta fazer (com foco especial no frontend que ainda tem partes quebradas), define uma sequência de execução recomendada e registra as melhorias de observabilidade (logs) no servidor InterfaceWithServer/RestServer.

## Visão Geral
- Backend: API REST funcional (CRUDs principais + chats + interesses + adoções). POST de animais foi implementado. Falta: upload de imagens e pequenos ajustes.
 - Frontend: Páginas existem; mocks já removidos em ONGs e Meus Matches; Meus Chats integrado (ADOTANTE) e filtragem para VOLUNTÁRIO no frontend.
- Observabilidade: Logging HTTP detalhado por requisição (método, path, status, bytes, duração, User-Agent) com `MPET_DEBUG`.

## Sequência Recomendada de Execução
1) Observabilidade & DX (hoje)
- [x] Ativar logs HTTP por requisição no `RestServer` (MPET_DEBUG).
- [x] Criar script `./run-server.sh` para facilitar runs locais.
- [x] Adicionar endpoint `/api/health` na home (widget de status) e checagem periódica.

2) Consertos de Navegação & Sessão (rápidos)
- [x] Revisar `router.js` e `navigation.js` para garantir:
  - [x] Proteção de rota consistente (ADOTANTE/VOLUNTARIO/ADMIN).
  - [x] Remover `alert()` e usar `showAlert()` unificado.
  - [x] Sincronizar navbar com `SessionManager` (estado logado/logoff).

3) Remover Mocks e Integrar APIs por Página
- [ ] `index.html`/`home.js`/`index.js`:
  - Trocar quaisquer referências a dados mockados -> `PetService` real.
  - Corrigir paginação e cartões.
- [ ] `match.html`:
  - Confirmar listagem real de animais (já funciona) e ações de interesse (POST /api/interesses).
  - Banner/empty-states coerentes.
- [x] `meus-matches.html`:
  - Já integrado ao backend (`PetService.getMyMatches`).
  - Validar render e status (PENDENTE/APROVADO/ADOTADO).
- [x] `meus-chats.html`:
  - [x] Listagem de threads via `/api/chats?cpfAdotante=...` (ADOTANTE).
  - [x] Envio/leitura de mensagens via `/api/chat-messages`.
  - [x] Botão fechar thread (`PUT /api/chats/:id/close`) e desabilitar input quando fechado.
  - [x] Filtragem de threads por ONG para VOLUNTÁRIO (animal.idOng = user.idOng) no frontend.
- [x] `registrar-usuario.html`:
  - Conectar formulários com `POST /api/adotantes` e `POST /api/voluntarios`.
  - Validações básicas e feedback de erro.
- [x] `registrar-ong.html`:
  - Conectar formulário com `POST /api/ongs`.
  - Validar CNPJ e campos obrigatórios.
- [ ] `login.html`:
  - Usar exclusivamente API de login (`/api/auth/login`) e remover fallbacks de mock no `SessionManager`.
- [ ] `sobre.html`:
  - Completar conteúdo e links úteis (API docs, GitHub, etc.).

4) Funcionais de Backend pendentes
- [x] `POST /api/animais` (criação via JSON – implementado).
- [ ] Upload de imagens dos animais:
  - Definir estratégia simples: URL pública (CDN/Unsplash placeholder) ou upload local (multipart) e servir de `public/uploads/`.
  - Atualizar modelo/API para armazenar `imageUrl`.
- [ ] Melhorar consistência de JSONs (usar Gson sistematicamente ao invés de concatenar strings). Prioritário nos endpoints mais usados.
- [ ] Endpoints de apoio para filtros (porte/tipo/sexo) se necessário.

5) UX & Polimento
- [ ] Empty states padronizados (com CTA) em todas as páginas.
- [ ] Mensagens de erro/sucesso unificadas (`showAlert`).
- [ ] Loading spinners onde existem chamadas encadeadas (matches, chats).

6) Testes Manuais (roteiro)
- [ ] Login: admin / adotante / voluntário.
- [ ] CRUD ONGs: criar, editar, remover.
- [ ] CRUD Animais: criar (quando implementado POST), listar, editar, remover.
- [ ] Interesses: criar, aprovar/recusar; Matches do adotante.
- [ ] Chats: criar thread ao aprovar, enviar/ler mensagens, fechar thread.
- [ ] Adoções: registrar e verificar impacto visual nos matches.
- [ ] Backup/restore (via CLI tradicional) e sanity check da API após restore.

## Itens Pontuais Encontrados (varredura rápida)
- `navigation.js` mantém mocks de ONGs (comentários: "FUTURO" e "MOCK"). Substituir por GET `/api/ongs`.
- `sessionManager.js` possui fallback de users mock quando a API falha. Remover comportamento em produção.
- `index.js` ainda referencia “camada de dados mockada” em comentários – revisar se sobrou algum uso real de mock.
- Diversos `alert()` diretos em `router.js` e `navigation.js` – padronizar em `showAlert()`.

## Observabilidade (Logs HTTP)
- Implementado wrapper de logging no `RestServer`:
  - Log de entrada: método, caminho, query, user-agent, contexto.
  - Log de saída: status HTTP, bytes da resposta e duração (ms).
- Toggle via env var: `MPET_DEBUG` (default `true`).

Exemplo de execução com logs:
```bash
# Build
mvn -f Codigo/pom.xml -DskipTests package

# Rodar com logs habilitados (default)
./run-server.sh

# Rodar com logs desabilitados
MPET_DEBUG=false ./run-server.sh
```

Formato dos logs:
```
[HTTP ►] GET /api/animais ua=curl/8.5.0 ctx=/api/animais
[HTTP ✔] GET /api/animais -> 200 (1234 bytes) 12ms
[HTTP ◄] handled in 12ms  ctx=/api/animais
```

## Ações Imediatas (1–2 dias)
- [x] Conectar `registrar-usuario.html` e `registrar-ong.html` às APIs.
- [x] Trocar mocks de ONGs por chamadas reais; sessão com alertas padronizados.
- [x] Implementar `POST /api/animais` (mínimo: nome, tipo, idOng).
- [~] Validar “Meus Chats” (ADOTANTE) end-to-end.

## Ações de Curto Prazo (3–5 dias)
- [ ] Upload de imagens (MVP local) + apresentação nas cards.
- [ ] Melhorias de UX (spinners, empty states, alertas padronizados).
- [ ] Sanitização de JSON (migrar concatenações para Gson onde faz sentido).

## Ações de Médio Prazo
- [ ] WebSocket para chat em tempo real (substituir polling).
- [ ] Testes automatizados básicos (API e UI smoke via Playwright).
- [ ] Otimizações no rebuild de índices e vacuum via CLI/API.

## Critérios de Aceite por Etapa
- Páginas sem mocks residuais e sem erros no console.
- Todas as rotas protegidas corretamente (redirecionam ao login quando necessário).
- Logs HTTP legíveis e úteis durante depuração.
- Fluxos principais OK: interesse → aprovação → chat → adoção → matches atualizados.

---
Documento mantido em `mds/PLANO_EXECUCAO_RESTANTE.md`. Atualize-o a cada avanço relevante.

## Registro de Progresso

2025-12-02
- Adicionado wrapper de logs HTTP (entrada/saída) em `RestServer` com `MPET_DEBUG` (default true).
- Criado script `run-server.sh` para facilitar execução com classpath (Gson/Commons).
- Health widget: `index.html` + `home.js` com polling de `/api/health` e indicador no header.
- Navegação e sessão: `router.js` e `navigation.js` ajustados para usar `showAlert`, proteção de rota e sincronização de navbar.
- Removido mock de ONGs em `navigation.js` e conectado a `GET /api/ongs`.
- Meus Matches: `meus-matches.html` usando dados reais (`PetService.getMyMatches`) e status de interesse/adoção.
- Backend: Implementado `POST /api/animais` (CACHORRO/GATO) com validações e retorno 201.
- Build e smoke test: servidor iniciado em `http://localhost:8080`, `GET /api/health`, `GET /api/ongs`, `GET /api/animais` ok.
- Meus Chats (ADOTANTE):
  - Lista threads via `/api/chats?cpfAdotante=...`, carrega nomes/tipos com `/api/animais`.
  - Abre thread e carrega mensagens via `/api/chats/:id/messages`.
  - Envia mensagens via `POST /api/chat-messages`.
  - Fecha conversa via `PUT /api/chats/:id/close`, bloqueando input e sinalizando no cabeçalho.
  - VOLUNTÁRIO: filtragem de threads por ONG aplicada no frontend.
