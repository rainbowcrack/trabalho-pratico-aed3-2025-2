# 🔄 API REST - Endpoints CRUD Completos

## ✅ Implementação Finalizada (2/12/2025)

Todos os endpoints PUT e DELETE foram implementados para completar o CRUD.

---

## 📍 Endpoints Disponíveis

### 🐾 **Animais** (`/api/animais`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| GET | `/api/animais` | Lista todos os animais | - |
| GET | `/api/animais/:id` | Busca animal por ID | - |
| PUT | `/api/animais/:id` | **NOVO** - Atualiza animal | `{nome, descricao, idOng}` |
| DELETE | `/api/animais/:id` | **NOVO** - Remove animal (soft delete) | - |

**Exemplo PUT:**
```bash
curl -X PUT http://localhost:8080/api/animais/1 \
  -H "Content-Type: application/json" \
  -d '{"nome": "Rex Atualizado", "descricao": "Muito brincalhão"}'
```

**Exemplo DELETE:**
```bash
curl -X DELETE http://localhost:8080/api/animais/1
```

---

### 🏢 **ONGs** (`/api/ongs`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| GET | `/api/ongs` | Lista todas as ONGs | - |
| POST | `/api/ongs` | Cria nova ONG | `{nome, cnpj, endereco, telefone}` |
| PUT | `/api/ongs/:id` | **NOVO** - Atualiza ONG | `{nome, cnpj, endereco, telefone}` |
| DELETE | `/api/ongs/:id` | **NOVO** - Remove ONG | - |

**Exemplo PUT:**
```bash
curl -X PUT http://localhost:8080/api/ongs/1 \
  -H "Content-Type: application/json" \
  -d '{"nome": "ONG Patinhas Felizes", "telefone": "11-99999-8888"}'
```

---

### 👤 **Adotantes** (`/api/adotantes`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| GET | `/api/adotantes/:cpf` | Busca adotante por CPF | - |
| POST | `/api/adotantes` | Cria novo adotante | `{cpf, senha, nomeCompleto, telefone?}` |
| PUT | `/api/adotantes/:cpf` | **NOVO** - Atualiza adotante | `{nomeCompleto?, telefone?, senha?}` |
| DELETE | `/api/adotantes/:cpf` | **NOVO** - Remove adotante | - |

**Exemplo PUT:**
```bash
curl -X PUT http://localhost:8080/api/adotantes/12345678900 \
  -H "Content-Type: application/json" \
  -d '{"telefone": "11-98888-7777", "senha": "novaSenha123"}'
```

---

### 🙋 **Voluntários** (`/api/voluntarios`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| GET | `/api/voluntarios/:cpf` | Busca voluntário por CPF | - |
| POST | `/api/voluntarios` | Cria novo voluntário | `{cpf, senha, nome, idOng, telefone?}` |
| PUT | `/api/voluntarios/:cpf` | **NOVO** - Atualiza voluntário | `{nome?, telefone?, senha?, endereco?}` |
| DELETE | `/api/voluntarios/:cpf` | **NOVO** - Remove voluntário | - |

**Exemplo PUT:**
```bash
curl -X PUT http://localhost:8080/api/voluntarios/98765432100 \
  -H "Content-Type: application/json" \
  -d '{"nome": "Maria Silva Santos", "telefone": "11-97777-6666"}'
```

---

### ❤️ **Interesses** (`/api/interesses`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| GET | `/api/interesses` | Lista todos os interesses | - |
| POST | `/api/interesses` | Registra novo interesse | `{cpfAdotante, idAnimal}` |
| PUT | `/api/interesses/:id` | **NOVO** - Aprovar/Recusar interesse | `{status: "APROVADO"/"RECUSADO"}` |
| DELETE | `/api/interesses/:id` | **NOVO** - Remove interesse | - |

**Exemplo PUT (Aprovar):**
```bash
curl -X PUT http://localhost:8080/api/interesses/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADO"}'
```

**Exemplo PUT (Recusar):**
```bash
curl -X PUT http://localhost:8080/api/interesses/2 \
  -H "Content-Type: application/json" \
  -d '{"status": "RECUSADO"}'
```

---

## 🔐 **Autenticação** (`/api/auth`)

| Método | Endpoint | Descrição | Body |
|--------|----------|-----------|------|
| POST | `/api/auth/login` | Login de usuário | `{cpf, senha}` |

---

## 📊 **Status da Implementação**

### ✅ **Concluído**
- [x] GET em todas as entidades
- [x] POST em todas as entidades (Animais ainda manual)
- [x] PUT em todas as entidades
- [x] DELETE em todas as entidades
- [x] Autenticação por CPF

### 🔄 **Próximas Melhorias**
- [ ] POST /api/animais (criar via JSON)
- [ ] GET /api/adotantes/:cpf/interesses (filtrar por usuário)
- [ ] GET /api/adotantes/:cpf/adocoes (filtrar por usuário)
- [ ] POST /api/chats (criar thread)
- [ ] POST /api/chat-messages (enviar mensagem)
- [ ] GET /api/chats/:threadId/messages
- [ ] WebSocket para chat em tempo real

---

## 🧪 **Testando os Endpoints**

### 1. Iniciar o servidor:
```bash
cd /home/pedrogaf/Secretária/trabalho-pratico-aed3-2025-2
make run-server
# ou
java -cp "Codigo/target/classes:Codigo/target/lib/*" br.com.mpet.InterfaceWithServer
```

### 2. Testar com curl:
```bash
# Listar animais
curl http://localhost:8080/api/animais

# Atualizar animal
curl -X PUT http://localhost:8080/api/animais/1 \
  -H "Content-Type: application/json" \
  -d '{"nome": "Buddy Atualizado"}'

# Deletar animal
curl -X DELETE http://localhost:8080/api/animais/1

# Aprovar interesse
curl -X PUT http://localhost:8080/api/interesses/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADO"}'
```

### 3. Testar no navegador:
Acesse: `http://localhost:8080/pages/index.html`

---

## 📝 **Notas Técnicas**

- **Soft Delete**: DELETE não remove fisicamente do arquivo, apenas marca com tombstone
- **CORS**: Todos os endpoints têm CORS habilitado (`Access-Control-Allow-Origin: *`)
- **Validation**: Campos obrigatórios são validados no backend
- **Error Handling**: Retorna JSON com `{"error": "mensagem"}` em caso de falha
- **Status Codes**:
  - `200 OK` - Sucesso
  - `201 Created` - Recurso criado
  - `404 Not Found` - Recurso não encontrado
  - `500 Internal Server Error` - Erro no servidor

---

## 🎯 **Uso no Frontend**

```javascript
// Atualizar animal
async function updateAnimal(id, data) {
  const response = await fetch(`http://localhost:8080/api/animais/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  return response.json();
}

// Deletar adotante
async function deleteAdotante(cpf) {
  const response = await fetch(`http://localhost:8080/api/adotantes/${cpf}`, {
    method: 'DELETE'
  });
  return response.json();
}

// Aprovar interesse
async function aprovarInteresse(id) {
  const response = await fetch(`http://localhost:8080/api/interesses/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'APROVADO' })
  });
  return response.json();
}
```

---

**Última Atualização**: 2 de dezembro de 2025
**Versão da API**: 1.0
**Backend**: Java 17 + HttpServer nativo
**Porta**: 8080
