# Relatório de Integração da Criptografia RSA

## Resumo Executivo
✅ **SIM, a criptografia RSA implementada é totalmente compatível com o projeto MPet.** Porém, requer pequenas adaptações no DAO de usuários para integração completa.

---

## 1. Análise da Arquitetura Atual

### 1.1 Armazenamento de Senhas
O projeto atualmente armazena senhas em **texto plano** nos arquivos `.dat`:
- **Arquivo**: `UsuarioDataFileDao.java`
- **Método**: `encodePayload()` → usa `Codec.encodeStringU16(u.getSenha())`
- **Locais**:
  - `Adotante` (adotantes.dat)
  - `Voluntário` (voluntarios.dat)

### 1.2 Processo de Autenticação (Interface.java)
```java
// Adotante: lê a senha e compara com texto plano
String senhaDigitada = ler senha do usuário
String senhaArmazenada = adotante.getSenha()
if (senhaDigitada.equals(senhaArmazenada)) { autenticado }

// Voluntário: mesmo processo
```

### 1.3 Formato Binário Atual (Codec)
```
[CPF(StringU16)][Senha(StringU16)][Telefone(StringU16)][...]
```

---

## 2. Compatibilidade da RSA Implementada

### ✅ Pontos Positivos

| Aspecto | Status | Motivo |
|---------|--------|--------|
| **Tamanho de Chaves** | ✅ OK | RSA-2048 adequado para senhas |
| **Base64 Encoding** | ✅ OK | Facilita armazenamento binário |
| **Assinatura Digital** | ✅ OK | Pode validar integridade de dados |
| **Carregamento de Chaves** | ✅ OK | Arquivo PEM reutilizável |

### ⚠️ Limitações e Considerações

1. **Limite de Tamanho (245 bytes)**
   - RSA-2048 suporta máximo ~245 bytes por mensagem
   - **Senhas**: 245 bytes é suficiente (senhas não excedem 128 chars UTF-8)
   - **Outros campos**: Também OK (CPF 11 chars, nomes até 200 chars)

2. **Assimetria de Chaves**
   - Chave privada necessária para criptografar/descriptografar
   - Segurança depende da proteção do arquivo `keys/private_key.pem`

3. **Performance**
   - RSA é ~1000x mais lento que XOR/AES
   - Para CRUD de usuários: aceitável
   - Para operações em batch: considerar cache ou hash

---

## 3. Plano de Integração (3 Opções)

### 🔐 OPÇÃO A: RSA para Senhas (Recomendado para Segurança Alta)

**Vantagens**: Máxima segurança assimétrica  
**Desvantagens**: Mais lento, precisa da chave privada em runtime

**Implementação**:
1. Modificar `UsuarioDataFileDao.encodePayload()`:
   ```java
   // Antes:
   byte[] senha = Codec.encodeStringU16(u.getSenha());
   
   // Depois:
   String senhaCriptografada = RSACriptografia.criptografar(u.getSenha());
   byte[] senha = Codec.encodeStringU16(senhaCriptografada);
   ```

2. Modificar `UsuarioDataFileDao.decodePayload()`:
   ```java
   // Antes:
   u.setSenha(dSenha.value);
   
   // Depois:
   try {
       String senhaOriginal = RSACriptografia.descriptografar(dSenha.value);
       u.setSenha(senhaOriginal);
   } catch (Exception e) {
       throw new IOException("Erro ao descriptografar senha", e);
   }
   ```

3. Modificar autenticação em `Interface.java`:
   ```java
   // Login Adotante
   String senhaDigitada = lerSenha();
   Adotante adotante = dao.lerPorCpf(cpf);
   
   if (senhaDigitada.equals(adotante.getSenha())) {
       // Autenticado
   }
   // Sem mudanças na lógica! A senha é descriptografada automaticamente
   ```

**Esforço**: 15 minutos  
**Segurança**: ⭐⭐⭐⭐⭐

---

### 🔐 OPÇÃO B: RSA com Hash (Recomendado para Performance)

**Vantagens**: Combina segurança (hash) com velocidade  
**Desvantagens**: Usa RSA apenas para integridade (não confidencialidade)

Usar RSA para:
- Assinar (sign) hashes SHA256 de senhas
- Verificar integridade dos dados no banco

**Implementação**: Menos intrusiva, usa apenas `assinar()` e `verificarAssinatura()`

**Esforço**: 20 minutos  
**Segurança**: ⭐⭐⭐⭐

---

### 🔐 OPÇÃO C: RSA Opcional via Flag

**Vantagens**: Compatível com dados existentes  
**Desvantagens**: Mais código condicional

Adicionar flag `senhaEncriptada` no payload:
```java
[CPF][senhaEncriptada(boolean)][Senha(StringU16)][Telefone][...]
```

Se `true` → descriptografa; se `false` → usa texto plano

**Esforço**: 30 minutos  
**Segurança**: ⭐⭐⭐⭐

---

## 4. Compatibilidade com Outras Funcionalidades

### 4.1 Persistência Binária (Codec)
✅ **Totalmente compatível**
- Codec suporta StringU16 com até 65534 bytes
- RSA criptografado em Base64 ≈ 344 bytes (2048-bit) → dentro do limite

### 4.2 Índices B+ Tree
✅ **Não afetados**
- B+ Tree indexa por ID, não por senha
- Criptografia é apenas no payload

### 4.3 Backup/Restore (ZIP)
✅ **Funciona normalmente**
- Dados backup já conterão senhas criptografadas
- Restore restaura dados criptografados

### 4.4 Vacuum (Compactação)
✅ **Totalmente compatível**
- Vacuum relê e reescreve dados
- Senhas permanecem criptografadas

### 4.5 Chat e Relacionamentos
⚠️ **Sem impacto direto**
- CPF em `ChatThread` e `Adoção` não precisa criptografia
- Referenciar por ID criptografado é overkill

---

## 5. Dados Que DEVEM Ser Criptografados

| Entidade | Campo | Prioridade | Motivo |
|----------|-------|------------|--------|
| Adotante | senha | ⭐⭐⭐⭐⭐ | Credencial de acesso |
| Voluntário | senha | ⭐⭐⭐⭐⭐ | Credencial de acesso |
| Adotante | CPF | ⭐⭐ | PII (informação pessoal) |
| Voluntário | CPF | ⭐⭐ | PII |
| Ong | cpfResponsavel | ⭐ | Opcional |

---

## 6. Dados que NÃO Precisam Ser Criptografados

| Campo | Motivo |
|-------|--------|
| Nome do animal | Público |
| Nome de ONG | Público |
| Telefone | Público na maioria dos casos |
| Data de adoção | Público |
| Status de interesse | Público |
| Mensagens de chat | Público/específico do thread |

---

## 7. Testes Recomendados Após Integração

```bash
# 1. Testar geração de chaves
java -cp Codigo/target/classes br.com.mpet.RSAKeyGen

# 2. Compilar projeto
mvn -f Codigo/pom.xml clean package -DskipTests

# 3. Executar testes manuais na CLI
# - Criar novo adotante com senha
# - Fazer login com a senha
# - Verificar arquivo adotantes.dat em binário (senha deve estar criptografada)
# - Fazer logout e login novamente
# - Tentar login com senha errada

# 4. Testar vacuum
# - Criar/deletar vários usuários
# - Executar vacuum
# - Fazer login após vacuum

# 5. Testar backup/restore
# - Fazer backup
# - Criar novo usuário
# - Restaurar backup anterior
# - Fazer login com usuário antigo
```

---

## 8. Solução Recomendada (OPÇÃO A - SIMPLES)

### Implementação Rápida em 3 Arquivos

**Arquivo 1: `UsuarioDataFileDao.java` - Método encodePayload()**
```java
// Para Adotante (linha ~293):
private static byte[] encodePayload(Adotante a) {
    try {
        String senhaCriptografada = RSACriptografia.criptografar(a.getSenha());
        byte[] senha = Codec.encodeStringU16(senhaCriptografada);
        // ... resto igual
    } catch (Exception e) {
        throw new RuntimeException("Erro ao criptografar senha", e);
    }
}

// Para Voluntário (linha ~316):
private static byte[] encodePayload(Voluntario v) {
    try {
        String senhaCriptografada = RSACriptografia.criptografar(v.getSenha());
        byte[] senha = Codec.encodeStringU16(senhaCriptografada);
        // ... resto igual
    } catch (Exception e) {
        throw new RuntimeException("Erro ao criptografar senha", e);
    }
}
```

**Arquivo 2: `UsuarioDataFileDao.java` - Método decodePayload()**
```java
// Ambos (Adotante e Voluntário, linha ~329 e ~375):
Codec.Decoded<String> dSenha = Codec.decodeStringU16(buf, off);
off = dSenha.nextOffset;

try {
    String senhaDescriptografada = RSACriptografia.descriptografar(dSenha.value);
    a.setSenha(senhaDescriptografada); // ou v.setSenha()
} catch (Exception e) {
    // Retrocompatibilidade: se falhar, assume texto plano (dados antigos)
    a.setSenha(dSenha.value);
}
```

**Arquivo 3: `Interface.java` - Gerar chaves no startup**
```java
// No método main(), antes de criar DAOs:
private static void inicializarChavesCriptografia() throws Exception {
    File keysDir = new File(System.getProperty("user.dir"), "keys");
    File privateKey = new File(keysDir, "private_key.pem");
    
    if (!privateKey.exists()) {
        System.out.println("⚙️  Gerando chaves RSA...");
        // Chamar RSAKeyGen
        RSAKeyGen.main(new String[]{});
    }
}
```

---

## 9. Verificação: "Funciona com Todo o Projeto?"

| Componente | Compatível | Notas |
|-----------|-----------|-------|
| DAO de Animais | ✅ Sim | Nenhuma mudança necessária |
| DAO de ONGs | ✅ Sim | Nenhuma mudança necessária |
| DAO de Adotantes | ⚠️ Requer 2 mudanças | Enconde/decode senhas |
| DAO de Voluntários | ⚠️ Requer 2 mudanças | Enconde/decode senhas |
| DAO de Adoções | ✅ Sim | Nenhuma mudança necessária |
| DAO de Interesses | ✅ Sim | Nenhuma mudança necessária |
| DAO de Chat | ✅ Sim | Nenhuma mudança necessária |
| Backup/Restore | ✅ Sim | Funciona com senhas criptografadas |
| Vacuum | ✅ Sim | Funciona com senhas criptografadas |
| B+ Tree Index | ✅ Sim | Não indexa senhas |
| CLI Interface | ✅ Sim | Nenhuma mudança na lógica de login |

---

## 10. Recomendações Finais

### ✅ RECOMENDAÇÕES

1. **Gerar chaves na primeira execução** (adicionar em `Interface.main()`)
2. **Integrar criptografia apenas em senhas** (OPÇÃO A)
3. **Manter retrocompatibilidade** (try-catch com fallback)
4. **Documentar chave privada como segredo** (.gitignore)
5. **Testar após cada mudança** (login, backup, vacuum)

### ⚠️ PRECAUÇÕES

1. **Nunca versione `keys/private_key.pem` no Git**
   ```
   # Adicionar ao .gitignore:
   keys/
   *.pem
   ```

2. **Em produção, use HSM (Hardware Security Module)**
   - AWS KMS
   - Azure Key Vault
   - YubiHSM

3. **Considere PBKDF2 ou bcrypt para senhas**
   - RSA é para criptografia assimétrica
   - Senhas devem usar KDF (Key Derivation Function)

---

## Conclusão

**✅ A criptografia RSA implementada é totalmente funcional e compatível com o projeto MPet.** 

Com apenas **2 pequenas mudanças no DAO de usuários**, você pode ativar criptografia de senhas RSA em todo o sistema sem afetar outras funcionalidades. O backup, restore, vacuum e índices B+ continuarão funcionando normalmente.

**Tempo de integração total: ~20-30 minutos**  
**Complexidade: Baixa**  
**Impacto no projeto: Mínimo**  
**Ganho em segurança: Significativo**
