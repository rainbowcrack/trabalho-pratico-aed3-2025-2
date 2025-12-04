# ✅ Integração Backend ↔ Frontend - Guia de Teste

## O que foi implementado (Alta Prioridade)

### 1. ✅ Backend - Endpoints POST
Foram adicionados os seguintes endpoints no `RestServer.java`:

- **POST /api/adotantes** - Criar adotante
- **POST /api/voluntarios** - Criar voluntário  
- **POST /api/ongs** - Criar ONG
- **POST /api/interesses** - Registrar interesse

### 2. ✅ Frontend - Integração Real
Substituídos dados mockados por chamadas fetch() reais:

- **petService.js** - Agora busca de `/api/animais`
- **registrar-usuario.html** - Envia POST para criar adotantes/voluntários
- **registrar-ong.html** - Envia POST para criar ONGs
- **Dropdown ONGs** - Carrega dinamicamente de `/api/ongs`

### 3. ✅ Dependências
- Adicionado **Gson 2.10.1** para parsing JSON no backend

---

## 🚀 Como Testar

### Passo 1: Compilar
```bash
cd trabalho-pratico-aed3-2025-2
mvn -f Codigo/pom.xml clean package -DskipTests
```

### Passo 2: Iniciar Servidor com API REST
```bash
make run-with-server
# OU
java -cp "Codigo/target/classes" br.com.mpet.InterfaceWithServer
```

Aguarde ver:
```
✨ PetMatch está pronto!
🌐 Frontend:  http://localhost:8080/pages/index.html
🔌 API REST:  http://localhost:8080/api
✅ Servidor iniciado com sucesso!
```

### Passo 3: Testar API (Terminal separado)

#### Health Check
```bash
curl http://localhost:8080/api/health
```
**Esperado:** `{"status":"ok","timestamp":"..."}`

#### Listar ONGs
```bash
curl http://localhost:8080/api/ongs
```
**Esperado:** Array JSON com ONGs cadastradas

#### Criar ONG
```bash
curl -X POST http://localhost:8080/api/ongs \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Patinhas Felizes",
    "cnpj": "12345678000199",
    "endereco": "Rua das Flores, 123",
    "telefone": "11987654321"
  }'
```
**Esperado:** `{"success":true,"message":"ONG criada com sucesso",...}`

#### Criar Adotante
```bash
curl -X POST http://localhost:8080/api/adotantes \
  -H "Content-Type: application/json" \
  -d '{
    "cpf": "98765432100",
    "senha": "senha123",
    "nomeCompleto": "João Silva",
    "telefone": "11987654321"
  }'
```
**Esperado:** `{"success":true,"message":"Adotante criado com sucesso",...}`

#### Login com Adotante
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "cpf": "98765432100",
    "senha": "senha123"
  }'
```
**Esperado:** Token + dados do usuário

#### Listar Animais
```bash
curl http://localhost:8080/api/animais
```
**Esperado:** Array JSON com animais (se houver seed data)

#### Registrar Interesse
```bash
curl -X POST http://localhost:8080/api/interesses \
  -H "Content-Type: application/json" \
  -d '{
    "cpfAdotante": "98765432100",
    "idAnimal": 1
  }'
```
**Esperado:** `{"success":true,"message":"Interesse registrado..."}`

---

## 🌐 Testar no Navegador

### 1. Abrir Frontend
```
http://localhost:8080/pages/index.html
```

### 2. Registrar ONG
- Ir em: `http://localhost:8080/pages/registrar-ong.html`
- Preencher formulário
- Clicar "Registrar ONG"
- **Verificar:** Alerta verde de sucesso + redirect para registro de usuário

### 3. Registrar Usuário (Adotante)
- Na página de registro, aba "Sou Adotante"
- Preencher CPF, senha, nome
- Clicar "Criar Conta como Adotante"
- **Verificar:** Alerta verde + redirect para login

### 4. Registrar Usuário (Voluntário)
- Aba "Sou Voluntário"
- **IMPORTANTE:** Dropdown de ONGs deve carregar automaticamente
- Selecionar ONG criada no passo 2
- Preencher dados
- Clicar "Criar Conta como Voluntário"
- **Verificar:** Alerta verde + redirect para login

### 5. Login
- Usar CPF e senha criados
- Clicar "Entrar"
- **Verificar:** Redirect para página match

### 6. Match de Pets
- Página deve carregar animais de `/api/animais`
- Clicar ❤️ (curtir)
- **Verificar:** 
  - Corações voando
  - Toast verde "Interesse registrado com sucesso"
  - Próximo animal aparece

---

## 🔍 Verificação de Dados

### Checar no CLI (Terminal do servidor)
Enquanto o servidor está rodando, você pode:
1. Escolher opção do menu CLI
2. Ver dados criados via API

### Checar arquivos .dat
```bash
ls -lh dats/
```
Arquivos devem aumentar de tamanho ao criar registros.

---

## 🐛 Troubleshooting

### Erro: "Failed to fetch"
- **Causa:** Backend não está rodando
- **Solução:** Verificar se `make run-with-server` está ativo

### Erro: "CPF ou senha incorretos"
- **Causa:** Usuário não existe ou senha errada
- **Solução:** Criar via POST /api/adotantes primeiro

### Dropdown de ONGs vazio
- **Causa:** Nenhuma ONG cadastrada
- **Solução:** Criar ONG via formulário ou POST /api/ongs

### Erro de compilação
- **Causa:** Maven cache ou Gson não baixado
- **Solução:** 
  ```bash
  mvn -f Codigo/pom.xml clean
  mvn -f Codigo/pom.xml dependency:resolve
  mvn -f Codigo/pom.xml compile
  ```

---

## ✅ Checklist Final

- [ ] Servidor inicia sem erros
- [ ] GET /api/health retorna OK
- [ ] GET /api/ongs retorna array
- [ ] POST /api/ongs cria ONG
- [ ] POST /api/adotantes cria adotante
- [ ] POST /api/auth/login autentica
- [ ] Frontend carrega animais de /api/animais
- [ ] Formulários enviam dados para API
- [ ] Dropdown de ONGs carrega dinamicamente
- [ ] Curtir animal registra interesse via POST

---

## 📊 Status Atual

| Funcionalidade | Backend | Frontend | Integrado |
|----------------|---------|----------|-----------|
| Listar Animais | ✅ GET | ✅ fetch | ✅ |
| Listar ONGs | ✅ GET | ✅ fetch | ✅ |
| Criar ONG | ✅ POST | ✅ form | ✅ |
| Criar Adotante | ✅ POST | ✅ form | ✅ |
| Criar Voluntário | ✅ POST | ✅ form | ✅ |
| Login | ✅ POST | ✅ form | ✅ |
| Registrar Interesse | ✅ POST | ✅ match | ✅ |

**Total: 7/7 funcionalidades de alta prioridade implementadas! 🎉**
