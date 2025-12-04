# 💬 Sistema de Chat - API REST

## ✅ Implementado (2/12/2025)

Sistema completo de chat em tempo real entre adotantes e voluntários sobre animais específicos.

---

## 📍 Endpoints de Chat Threads

### 🔹 **Listar Threads** 
**GET** `/api/chats`

Lista todas as threads de chat. Suporta filtros opcionais.

**Query Parameters:**
- `cpfAdotante` (opcional) - Filtrar por CPF do adotante
- `idAnimal` (opcional) - Filtrar por ID do animal

**Exemplo:**
```bash
# Todas as threads
curl http://localhost:8080/api/chats

# Threads de um adotante específico
curl http://localhost:8080/api/chats?cpfAdotante=12345678900

# Threads sobre um animal específico
curl http://localhost:8080/api/chats?idAnimal=5
```

**Resposta:**
```json
[
  {
    "id": 1,
    "idAnimal": 5,
    "cpfAdotante": "12345678900",
    "aberto": true,
    "criadoEm": 1701518400000
  }
]
```

---

### 🔹 **Buscar Thread Específica**
**GET** `/api/chats/:threadId`

Busca uma thread de chat pelo ID.

**Exemplo:**
```bash
curl http://localhost:8080/api/chats/1
```

**Resposta:**
```json
{
  "id": 1,
  "idAnimal": 5,
  "cpfAdotante": "12345678900",
  "aberto": true,
  "criadoEm": 1701518400000
}
```

---

### 🔹 **Listar Mensagens de uma Thread**
**GET** `/api/chats/:threadId/messages`

Lista todas as mensagens de uma thread específica.

**Exemplo:**
```bash
curl http://localhost:8080/api/chats/1/messages
```

**Resposta:**
```json
[
  {
    "id": 1,
    "threadId": 1,
    "sender": "ADOTANTE",
    "conteudo": "Olá! Gostaria de saber mais sobre o Rex.",
    "enviadoEm": 1701518460000
  },
  {
    "id": 2,
    "threadId": 1,
    "sender": "VOLUNTARIO",
    "conteudo": "Olá! O Rex é muito carinhoso e brincalhão.",
    "enviadoEm": 1701518520000
  }
]
```

---

### 🔹 **Criar Nova Thread**
**POST** `/api/chats`

Cria uma nova thread de chat entre adotante e voluntário sobre um animal.

**Body:**
```json
{
  "idAnimal": 5,
  "cpfAdotante": "12345678900"
}
```

**Comportamento:**
- Se já existir uma thread para o mesmo `idAnimal` + `cpfAdotante`, retorna a thread existente
- Caso contrário, cria uma nova thread com status `aberto: true`

**Exemplo:**
```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -d '{"idAnimal": 5, "cpfAdotante": "12345678900"}'
```

**Resposta (201 Created):**
```json
{
  "id": 1,
  "idAnimal": 5,
  "cpfAdotante": "12345678900",
  "aberto": true,
  "criadoEm": 1701518400000
}
```

---

### 🔹 **Fechar Thread**
**PUT** `/api/chats/:threadId/close`

Fecha uma thread de chat (define `aberto: false`). Mensagens não podem mais ser enviadas em threads fechadas.

**Exemplo:**
```bash
curl -X PUT http://localhost:8080/api/chats/1/close
```

**Resposta:**
```json
{
  "success": true,
  "message": "Thread fechada com sucesso"
}
```

---

## 📍 Endpoints de Mensagens

### 🔹 **Listar Mensagens**
**GET** `/api/chat-messages`

Lista todas as mensagens. Suporta filtro por thread.

**Query Parameters:**
- `threadId` (opcional) - Filtrar por ID da thread

**Exemplo:**
```bash
# Todas as mensagens
curl http://localhost:8080/api/chat-messages

# Mensagens de uma thread específica
curl http://localhost:8080/api/chat-messages?threadId=1
```

**Resposta:**
```json
[
  {
    "id": 1,
    "threadId": 1,
    "sender": "ADOTANTE",
    "conteudo": "Olá! Gostaria de saber mais sobre o Rex.",
    "enviadoEm": 1701518460000
  }
]
```

---

### 🔹 **Enviar Mensagem**
**POST** `/api/chat-messages`

Envia uma nova mensagem em uma thread.

**Body:**
```json
{
  "threadId": 1,
  "sender": "ADOTANTE",
  "conteudo": "Qual é o temperamento dele?"
}
```

**Validações:**
- Thread deve existir (404 se não encontrada)
- Thread deve estar aberta (400 se fechada)
- `sender` deve ser "ADOTANTE" ou "VOLUNTARIO"

**Exemplo:**
```bash
curl -X POST http://localhost:8080/api/chat-messages \
  -H "Content-Type: application/json" \
  -d '{
    "threadId": 1,
    "sender": "ADOTANTE",
    "conteudo": "Qual é o temperamento dele?"
  }'
```

**Resposta (201 Created):**
```json
{
  "id": 3,
  "threadId": 1,
  "sender": "ADOTANTE",
  "conteudo": "Qual é o temperamento dele?",
  "enviadoEm": 1701518580000
}
```

**Erros:**
```json
// Thread não encontrada
{
  "error": "Thread não encontrada"
}

// Thread fechada
{
  "error": "Thread está fechada"
}
```

---

## 📝 Tipos de Dados

### ChatThread
```typescript
{
  id: number;           // ID único da thread
  idAnimal: number;     // ID do animal sobre o qual se conversa
  cpfAdotante: string;  // CPF do adotante participante
  aberto: boolean;      // Se a thread aceita novas mensagens
  criadoEm: number;     // Timestamp (epoch millis)
}
```

### ChatMessage
```typescript
{
  id: number;          // ID único da mensagem
  threadId: number;    // ID da thread à qual pertence
  sender: "ADOTANTE" | "VOLUNTARIO";  // Quem enviou
  conteudo: string;    // Texto da mensagem
  enviadoEm: number;   // Timestamp (epoch millis)
}
```

---

## 🎯 Fluxo de Uso

### 1. **Adotante demonstra interesse em animal**
```javascript
// 1. Registrar interesse
await fetch('http://localhost:8080/api/interesses', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    cpfAdotante: '12345678900',
    idAnimal: 5
  })
});
```

### 2. **Voluntário aprova o interesse**
```javascript
// 2. Aprovar interesse
await fetch('http://localhost:8080/api/interesses/1', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ status: 'APROVADO' })
});
```

### 3. **Sistema cria thread de chat automaticamente**
```javascript
// 3. Criar thread (pode ser feito automaticamente ao aprovar)
const response = await fetch('http://localhost:8080/api/chats', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    idAnimal: 5,
    cpfAdotante: '12345678900'
  })
});
const thread = await response.json();
```

### 4. **Adotante e voluntário conversam**
```javascript
// 4. Enviar mensagens
await fetch('http://localhost:8080/api/chat-messages', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    threadId: thread.id,
    sender: 'ADOTANTE',
    conteudo: 'Olá! Gostaria de agendar uma visita.'
  })
});
```

### 5. **Voluntário finaliza o chat após adoção**
```javascript
// 5. Fechar thread
await fetch(`http://localhost:8080/api/chats/${thread.id}/close`, {
  method: 'PUT'
});
```

---

## 🚀 Exemplo: Interface de Chat Simples

```html
<!DOCTYPE html>
<html>
<head>
  <title>Chat PetMatch</title>
  <style>
    #messages { height: 400px; overflow-y: scroll; border: 1px solid #ccc; padding: 10px; }
    .message { margin: 10px 0; padding: 8px; border-radius: 4px; }
    .ADOTANTE { background: #e3f2fd; text-align: left; }
    .VOLUNTARIO { background: #f3e5f5; text-align: right; }
  </style>
</head>
<body>
  <h2>Chat sobre Rex (ID: 5)</h2>
  <div id="messages"></div>
  <input type="text" id="messageInput" placeholder="Digite sua mensagem..." style="width: 80%;">
  <button onclick="sendMessage()">Enviar</button>

  <script>
    const THREAD_ID = 1;
    const CURRENT_USER = 'ADOTANTE'; // ou 'VOLUNTARIO'
    
    // Carregar mensagens ao abrir
    async function loadMessages() {
      const response = await fetch(`http://localhost:8080/api/chats/${THREAD_ID}/messages`);
      const messages = await response.json();
      
      const container = document.getElementById('messages');
      container.innerHTML = '';
      
      messages.forEach(msg => {
        const div = document.createElement('div');
        div.className = `message ${msg.sender}`;
        const time = new Date(msg.enviadoEm).toLocaleTimeString();
        div.innerHTML = `<strong>${msg.sender}:</strong> ${msg.conteudo} <small>(${time})</small>`;
        container.appendChild(div);
      });
      
      container.scrollTop = container.scrollHeight;
    }
    
    // Enviar mensagem
    async function sendMessage() {
      const input = document.getElementById('messageInput');
      const conteudo = input.value.trim();
      
      if (!conteudo) return;
      
      await fetch('http://localhost:8080/api/chat-messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          threadId: THREAD_ID,
          sender: CURRENT_USER,
          conteudo: conteudo
        })
      });
      
      input.value = '';
      loadMessages();
    }
    
    // Atualizar a cada 3 segundos (polling simples)
    setInterval(loadMessages, 3000);
    loadMessages();
  </script>
</body>
</html>
```

---

## 🔄 Atualização em Tempo Real

### **Opção 1: Polling (Implementada)**
```javascript
// Atualizar a cada 3 segundos
setInterval(() => {
  fetch(`/api/chats/${threadId}/messages`)
    .then(r => r.json())
    .then(messages => updateUI(messages));
}, 3000);
```

### **Opção 2: Long Polling (Futuro)**
```javascript
async function longPoll() {
  const response = await fetch(`/api/chats/${threadId}/messages?since=${lastMessageId}`);
  const newMessages = await response.json();
  updateUI(newMessages);
  longPoll(); // Recursivo
}
```

### **Opção 3: WebSocket (Futuro)**
```javascript
const ws = new WebSocket('ws://localhost:8080/api/chats/ws');
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  addMessageToUI(message);
};
```

---

## 📊 Integração com Outras Funcionalidades

### **Criar thread ao aprovar interesse**
```javascript
async function aprovarInteresseEAbrirChat(interesseId, idAnimal, cpfAdotante) {
  // 1. Aprovar interesse
  await fetch(`/api/interesses/${interesseId}`, {
    method: 'PUT',
    body: JSON.stringify({ status: 'APROVADO' })
  });
  
  // 2. Criar thread automaticamente
  const response = await fetch('/api/chats', {
    method: 'POST',
    body: JSON.stringify({ idAnimal, cpfAdotante })
  });
  
  return await response.json();
}
```

### **Notificar novo match**
```javascript
// Quando voluntário aprova interesse
function notificarNovoMatch(thread) {
  // Enviar notificação push
  // Enviar email
  // Mostrar badge na UI
}
```

---

## ✅ **Status: COMPLETO**

**Implementado:**
- ✅ GET /api/chats (com filtros)
- ✅ GET /api/chats/:id
- ✅ GET /api/chats/:id/messages
- ✅ POST /api/chats (criar thread)
- ✅ PUT /api/chats/:id/close
- ✅ GET /api/chat-messages (com filtro)
- ✅ POST /api/chat-messages (enviar)
- ✅ Validação de thread aberta/fechada
- ✅ Prevenção de threads duplicadas
- ✅ Timestamps em epoch millis

**Próximas Melhorias:**
- [ ] WebSocket para tempo real
- [ ] Indicador "digitando..."
- [ ] Confirmação de leitura
- [ ] Histórico de mensagens paginado
- [ ] Upload de imagens no chat
- [ ] Notificações push

---

**Última Atualização**: 2 de dezembro de 2025  
**Versão da API**: 1.0  
**Backend**: Java 17 + HttpServer nativo  
**Porta**: 8080
