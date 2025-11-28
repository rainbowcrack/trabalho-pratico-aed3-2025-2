# ✅ Integração REST Completa - Resumo

## O Que Foi Feito

### 1. **InterfaceWithServer.java** (Nova classe)
- Classe wrapper que inicia tanto o CLI quanto o REST Server
- Instancia todos os 8 DAOs (Animal, ONG, Adotante, etc.)
- Cria `RestServer` e chama `start()` em thread paralela
- Permite usar CLI e API REST simultaneamente
- Resguarda porta: **localhost:8080**

### 2. **RestServer.java** (Já existente, corrigido)
- Servidor HTTP com `com.sun.net.httpserver.HttpServer`
- 8 handlers para endpoints principais:
  - `/api/health` - Health check
  - `/api/auth/login` - Autenticação
  - `/api/animais` - CRUD de animais
  - `/api/ongs` - Lista de ONGs
  - `/api/adotantes` - Adotantes por CPF
  - `/api/voluntarios` - Voluntários por CPF
  - `/api/interesses` - Interesses
  - `/api/chats` - Threads de chat
  - `/api/chat-messages` - Mensagens

### 3. **Makefile** (Atualizado)
- Novo target: `make run-with-server`
- Compila e executa InterfaceWithServer

### 4. **pom.xml** (Corrigido)
- Versão Java downgrade: 21 → 17 (compatível com ambiente)

### 5. **Documentação**
- `SERVIDOR_REST.md` - Referência de endpoints e uso
- `GUIA_TESTE.md` - Guia completo com exemplos de teste

---

## 🎯 Como Usar

### Terminal 1: Iniciar Servidor
```bash
cd trabalho-pratico-aed3-2025-2
make build       # Compilar
make run-with-server  # Executar com REST API
```

Saída esperada:
```
✨ PetMatch está pronto!
🌐 Frontend:  http://localhost:8080/pages/index.html
🔌 API REST:  http://localhost:8080/api
✅ Servidor iniciado com sucesso!

... (menu CLI aparece)
```

### Terminal 2: Testar API
```bash
# Health check
curl http://localhost:8080/api/health

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf":"admin","senha":"admin"}'

# Listar animais
curl http://localhost:8080/api/animais

# Listar ONGs
curl http://localhost:8080/api/ongs
```

---

## 🔗 Fluxo de Integração

```
Frontend (HTML/JS)
      ↓
   fetch() → http://localhost:8080/api/*
      ↓
  RestServer
      ↓
   DAOs (AnimalDataFileDao, etc)
      ↓
  .dat files (dats/ directory)
```

---

## 📋 Checklist de Implementação

✅ **Fase 1: Infrastructure**
- [x] RestServer classe criada
- [x] InterfaceWithServer criada
- [x] Makefile atualizado
- [x] Compilação funcional

✅ **Fase 2: Endpoints GET**
- [x] /api/health
- [x] /api/auth/login (POST)
- [x] /api/animais
- [x] /api/ongs
- [x] /api/adotantes/:cpf
- [x] /api/voluntarios/:cpf
- [x] /api/interesses
- [x] /api/chats
- [x] /api/chat-messages
- [x] CORS headers

⏳ **Fase 3: Endpoints POST (TODO)**
- [ ] POST /api/adotantes (registrar)
- [ ] POST /api/voluntarios (registrar)
- [ ] POST /api/ongs (criar ONG)
- [ ] POST /api/chats (iniciar chat)
- [ ] POST /api/chat-messages (enviar mensagem)

⏳ **Fase 4: Endpoints PUT/DELETE (TODO)**
- [ ] PUT /api/animais/:id
- [ ] DELETE /api/animais/:id
- [ ] Etc para outros recursos

⏳ **Fase 5: Frontend Integration (TODO)**
- [ ] Atualizar login.html para usar /api/auth/login
- [ ] Criar petService.js com fetch() real
- [ ] Atualizar meus-matches.html para carregar via API
- [ ] Atualizar meus-chats.html para carregar via API
- [ ] Conectar formulários de registro a endpoints POST

---

## 🔑 Credenciais de Teste

### Admin
- **CPF:** `admin`
- **Senha:** `admin`
- **Resposta esperada:**
  ```json
  {
    "success": true,
    "token": "mock_token_admin",
    "user": {
      "cpf": "admin",
      "nome": "Administrador",
      "role": "ADMIN"
    }
  }
  ```

### Usuários Reais
Após rodar seed data, use CPFs cadastrados com suas respectivas senhas

---

## 📂 Estrutura de Arquivos

```
trabalho-pratico-aed3-2025-2/
├── Makefile                          # targets: build, run, run-with-server
├── SERVIDOR_REST.md                  # Documentação técnica
├── GUIA_TESTE.md                     # Guia com exemplos
├── Codigo/
│   ├── pom.xml                       # java.version: 17
│   ├── src/main/java/br/com/mpet/
│   │   ├── InterfaceWithServer.java  # ✨ Nova classe
│   │   ├── RestServer.java           # ✨ Corrigido
│   │   ├── Interface.java            # CLI existente
│   │   ├── model/                    # Entidades
│   │   └── persistence/              # DAOs
│   └── target/classes/               # Compilado
└── dats/                             # Dados (criado em tempo de execução)
    ├── animais.dat
    ├── animais.dat.idx
    ├── ongs.dat
    ├── ongs.dat.idx
    └── ... (6 outros pares de arquivos)
```

---

## 🧪 Próximas Mudanças Necessárias

### 1. Adicionar POST para criação de recursos
```java
// Em RestServer.java - handler para POST /api/adotantes
private class CriarAdotanteHandler implements HttpHandler {
    // Ler JSON body
    // Criar novo Adotante
    // Chamar adotanteDao.create(adotante)
    // Retornar JSON com novo ID
}
```

### 2. Atualizar frontend para chamar API
```javascript
// Em login.html
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const cpf = document.getElementById('cpf').value;
    const senha = document.getElementById('senha').value;
    
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cpf, senha })
    });
    
    if (response.ok) {
        const data = await response.json();
        SessionManager.login(data.user, data.token);
        window.location.href = 'index.html';
    }
});
```

### 3. Criar petService.js real
```javascript
// Em Codigo/src/main/resources/public/assets/js/services/petService.js
async function getAnimals() {
    return fetch('/api/animais').then(r => r.json());
}

async function getONGs() {
    return fetch('/api/ongs').then(r => r.json());
}

async function getAdotante(cpf) {
    return fetch(`/api/adotantes/${cpf}`).then(r => r.json());
}
```

---

## 📊 Métricas

| Item | Status |
|------|--------|
| RestServer funcional | ✅ |
| InterfaceWithServer criada | ✅ |
| Compilação limpa | ✅ |
| 9 endpoints GET | ✅ |
| 1 endpoint POST | ✅ |
| CORS habilitado | ✅ |
| Documentação | ✅ |
| Testes manuais | ⏳ Pendente |
| Frontend integrado | ⏳ Pendente |
| POST endpoints completos | ⏳ Pendente |

---

## 🚀 Próximo Passo

Execute:
```bash
make build && make run-with-server
```

Depois teste em outro terminal:
```bash
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf":"admin","senha":"admin"}'
```

Sucesso! 🎉

