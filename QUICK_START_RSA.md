# 🚀 Guia Rápido - Criptografia RSA Integrada

## ⚡ Uso Imediato

### 1️⃣ Compilar o Projeto
```bash
cd Codigo
# Use uma das opções abaixo:
mvn clean package -DskipTests      # Se Maven está instalado
make build                           # Se Make está instalado
```

### 2️⃣ Executar a Aplicação
```bash
java -cp Codigo/target/classes br.com.mpet.Interface
```

**Primeira execução**: Verá a mensagem
```
⚙️  Gerando par de chaves RSA-2048...
✓ Chaves RSA inicializadas com sucesso!
```

### 3️⃣ Arquivos Criados Automaticamente
- `keys/public_key.pem` - Chave pública (pode compartilhar)
- `keys/private_key.pem` - Chave privada **(CONFIDENCIAL)**

---

## 🔐 Segurança

### ⚠️ IMPORTANTE: Proteja a Chave Privada!

```bash
# NÃO versione a chave privada
git status
# Verificar que "keys/" está listado como ignorado

# Verificar permissões de arquivo (Linux/Mac)
ls -la keys/
# Deve mostrar: -rw------- (somente leitura para proprietário)

# No Windows
icacls keys\private_key.pem /inheritance:r /grant:r "%USERNAME%:F"
```

---

## 💾 Fluxo de Dados

### Senhas de Usuários

**Criação de novo usuário**:
```
Admin cria novo Adotante com senha "senha123"
    ↓
RSA criptografa: "senha123" → "MIIEowIBAAKCAQEA..."
    ↓
Armazena em adotantes.dat (criptografado)
```

**Login**:
```
Adotante tenta login com CPF + "senha123"
    ↓
RSA descriptografa dado armazenado
    ↓
Compara "senha123" == "senha123" ✓
    ↓
Login bem-sucedido
```

---

## 🧪 Testes Rápidos

### Teste 1: Criar e Logar
```
1. Menu: 1 (Admin) / admin / admin
2. 1 → Gerenciar Adotantes
3. 1 → Criar novo
4. CPF: 123.456.789-10
5. Senha: teste123
6. Logout (0)
7. Login como adotante: 123.456.789-10 / teste123
```

### Teste 2: Tentar Senha Errada
```
1. Login: 123.456.789-10 / senhaerrada
2. Esperado: "Falha na autenticação"
```

### Teste 3: Verificar Arquivo Binário
```bash
# Ver que adotantes.dat contém dados criptografados (não legíveis)
file dats/adotantes.dat
# Se contiver texto legível, é modo compatibilidade (dados antigos)
```

---

## 📊 Estrutura de Diretórios

```
projeto/
├── Codigo/
│   ├── src/main/java/br/com/mpet/
│   │   ├── Interface.java (modificado)
│   │   ├── RSAKeyGen.java (novo)
│   │   ├── RSACriptografia.java (novo)
│   │   └── persistence/dao/
│   │       └── UsuarioDataFileDao.java (modificado)
│   ├── target/classes/
│   └── pom.xml
├── dats/ (dados criptografados)
│   ├── adotantes.dat
│   ├── adotantes.dat.idx
│   ├── voluntarios.dat
│   └── ...
├── keys/ (CONFIDENCIAL)
│   ├── public_key.pem
│   └── private_key.pem ⚠️
└── .gitignore (inclui keys/)
```

---

## 🐛 Se Algo Der Errado

### Problema: "Falha ao inicializar chaves RSA"

**Causa**: Permissões de arquivo ou diretório ausente

**Solução**:
```bash
# Deletar diretório keys e tentar novamente
rm -rf keys/
java -cp Codigo/target/classes br.com.mpet.Interface
```

### Problema: Login falha mesmo com senha correta

**Causa 1**: Dados em modo compatibilidade (texto plano)

**Solução**: 
- Editar usuario e salvar novamente (ativa criptografia RSA)

**Causa 2**: Chaves geradas depois que dados foram criados

**Solução**:
```bash
# Restaurar de backup anterior
java -cp Codigo/target/classes br.com.mpet.Interface
# Menu Admin → Sistema → Restaurar de backup
```

### Problema: "Cannot find br.com.mpet.RSACriptografia"

**Causa**: Compilação incompleta

**Solução**:
```bash
cd Codigo
mvn clean compile
cd ..
java -cp Codigo/target/classes br.com.mpet.Interface
```

---

## 🔄 Migrando Dados Antigos

Se você tinha dados em **texto plano** antes da integração:

### Opção 1: Automática (Recomendada)
1. Execute a aplicação
2. Chaves RSA serão geradas
3. Dados antigos são lidos em modo compatibilidade
4. Primeira vez que um usuário for salvo → criptografia ativa

### Opção 2: Manual
```bash
# 1. Backup dos dados antigos
copy dats\adotantes.dat adotantes.dat.backup

# 2. Deletar dados antigos
del dats\*

# 3. Executar e criar novos dados
java -cp Codigo/target/classes br.com.mpet.Interface
```

---

## 📚 Documentação Completa

Para detalhes técnicos, veja:
- `GUIA_RSA.md` - Manual técnico
- `RELATORIO_RSA_INTEGRACAO.md` - Análise técnica
- `MUDANCAS_RSA_IMPLEMENTADAS.md` - Histórico de mudanças

---

## ✅ Checklist de Implementação

- [x] Código compilado sem erros
- [x] Chaves RSA geradas automaticamente
- [x] Senhas criptografadas ao salvar
- [x] Senhas descriptografadas ao carregar
- [x] Login funciona normalmente
- [x] Retrocompatibilidade com dados antigos
- [x] Arquivo .gitignore protege chave privada
- [x] Documentação completa

---

## 🎯 Próximas Ações (Opcional)

1. **Testar em Produção**
   - Fazer backup de dados em produção
   - Executar aplicação em ambiente de testes
   - Validar funcionamento

2. **Implementar Assinatura Digital** (Opcional)
   - Proteger integridade de dados
   - Usar `RSACriptografia.assinar()` / `verificarAssinatura()`

3. **Auditar Acesso** (Recomendado)
   - Registrar quando usuários acessam dados sensíveis
   - Implementar log de operações criptográficas

4. **Produção** (Fase Final)
   - Usar HSM (Hardware Security Module)
   - Implementar rotação de chaves
   - Compliance com LGPD/GDPR

---

## 📞 Suporte

Se encontrar problemas:
1. Verifique se `keys/` foi criado
2. Verifique se `RSACriptografia.java` está compilado
3. Verifique permissões de arquivo
4. Consulte os guias técnicos na documentação

---

**Status**: ✅ Integração completa e funcional  
**Versão**: 1.0  
**Data**: 22 de Novembro de 2025  
**Autor**: GitHub Copilot
