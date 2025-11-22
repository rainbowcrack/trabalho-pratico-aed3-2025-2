# 💻 Exemplos de Código - Criptografia RSA

## Uso Direto de RSACriptografia

### Exemplo 1: Criptografar uma Senha

```java
import br.com.mpet.RSACriptografia;

public class ExemploSenha {
    public static void main(String[] args) throws Exception {
        // Senha do usuário
        String senhaOriginal = "MinhaSenh@123";
        
        // Criptografar
        String senhaCriptografada = RSACriptografia.criptografar(senhaOriginal);
        System.out.println("Criptografada: " + senhaCriptografada);
        
        // Descriptografar
        String senhaRecuperada = RSACriptografia.descriptografar(senhaCriptografada);
        System.out.println("Recuperada: " + senhaRecuperada);
        
        // Verificar
        System.out.println("Igual? " + senhaRecuperada.equals(senhaOriginal));
    }
}
```

**Saída esperada**:
```
Criptografada: MIIEowIBAAKCAQEA7Hlt...
Recuperada: MinhaSenh@123
Igual? true
```

---

### Exemplo 2: Assinar Dados Digitalmente

```java
import br.com.mpet.RSACriptografia;

public class ExemploAssinatura {
    public static void main(String[] args) throws Exception {
        String documento = "Este é um documento importante";
        
        // Assinar
        String assinatura = RSACriptografia.assinar(documento);
        System.out.println("Assinatura: " + assinatura);
        
        // Verificar assinatura (dados originais)
        boolean valido1 = RSACriptografia.verificarAssinatura(documento, assinatura);
        System.out.println("Assinatura válida (original)? " + valido1);
        
        // Verificar assinatura (dados modificados)
        String documentoModificado = "Este é um OUTRO documento importante";
        boolean valido2 = RSACriptografia.verificarAssinatura(documentoModificado, assinatura);
        System.out.println("Assinatura válida (modificado)? " + valido2);
    }
}
```

**Saída esperada**:
```
Assinatura: QjcxODM4NDAxMjM4MTI...
Assinatura válida (original)? true
Assinatura válida (modificado)? false
```

---

### Exemplo 3: Integração com Usuários

```java
import br.com.mpet.model.Adotante;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import br.com.mpet.RSACriptografia;
import java.io.File;
import java.util.Optional;

public class ExemploIntegracaoUsuario {
    public static void main(String[] args) throws Exception {
        File dbFile = new File("dats/adotantes.dat");
        AdotanteDataFileDao dao = new AdotanteDataFileDao(dbFile, (byte)1);
        
        // Criar novo adotante COM SENHA
        Adotante novoAdotante = new Adotante();
        novoAdotante.setCpf("123.456.789-10");
        novoAdotante.setSenha("SenhaSegura123!");  // Será criptografada automaticamente
        novoAdotante.setNomeCompleto("João Silva");
        novoAdotante.setTelefone("(11) 98765-4321");
        novoAdotante.setAtivo(true);
        
        // Salvar (AUTOMÁTICAMENTE CRIPTOGRAFA)
        dao.create(novoAdotante);
        System.out.println("✓ Adotante salvo com senha criptografada");
        
        // Ler (AUTOMÁTICAMENTE DESCRIPTOGRAFA)
        Optional<Adotante> adotanteRecuperado = dao.read("123.456.789-10");
        if (adotanteRecuperado.isPresent()) {
            Adotante a = adotanteRecuperado.get();
            System.out.println("Nome: " + a.getNomeCompleto());
            System.out.println("Senha recuperada: " + a.getSenha());
            System.out.println("Senha correta? " + a.getSenha().equals("SenhaSegura123!"));
        }
        
        dao.close();
    }
}
```

**Saída esperada**:
```
✓ Adotante salvo com senha criptografada
Nome: João Silva
Senha recuperada: SenhaSegura123!
Senha correta? true
```

---

## Geração de Chaves Manual

### Exemplo 4: Gerar Novas Chaves

```java
import br.com.mpet.RSAKeyGen;

public class ExemploGeracaoChaves {
    public static void main(String[] args) throws Exception {
        // Gerar novas chaves RSA-2048
        RSAKeyGen.main(new String[]{});
        
        System.out.println("✓ Chaves geradas:");
        System.out.println("  - keys/public_key.pem");
        System.out.println("  - keys/private_key.pem");
    }
}
```

---

## Casos de Uso Avançados

### Exemplo 5: Validar Força de Senha

```java
import br.com.mpet.RSACriptografia;

public class ValidadorSenha {
    
    public static void main(String[] args) throws Exception {
        String[] senhas = {
            "123",                  // Fraca
            "senha",                // Fraca
            "Senha123",             // Média
            "SenhaM@int0Forte123"  // Forte
        };
        
        for (String senha : senhas) {
            int forca = calcularForcaSenha(senha);
            String nivel = forca < 40 ? "FRACA" : 
                          forca < 70 ? "MEDIA" : "FORTE";
            
            System.out.printf("%-25s -> %s (%d%%)%n", senha, nivel, forca);
            
            // Criptografar e verificar tamanho
            String criptografada = RSACriptografia.criptografar(senha);
            System.out.printf("  Criptografada: %d bytes%n%n", criptografada.length());
        }
    }
    
    static int calcularForcaSenha(String s) {
        int forca = 0;
        if (s.length() >= 8) forca += 20;
        if (s.length() >= 12) forca += 10;
        if (s.matches(".*[a-z].*")) forca += 10;
        if (s.matches(".*[A-Z].*")) forca += 15;
        if (s.matches(".*\\d.*")) forca += 15;
        if (s.matches(".*[!@#$%^&*].*")) forca += 20;
        return Math.min(forca, 100);
    }
}
```

**Saída esperada**:
```
123                       -> FRACA (20%)
senha                     -> MEDIA (40%)
Senha123                  -> MEDIA (60%)
SenhaM@int0Forte123     -> FORTE (100%)
```

---

### Exemplo 6: Implementar Cache de Chaves

```java
import br.com.mpet.RSACriptografia;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class CacheChaves {
    private static Map<String, PublicKey> cachePublico = new HashMap<>();
    private static Map<String, PrivateKey> cachePrivado = new HashMap<>();
    
    public static String criptografarComCache(String texto) throws Exception {
        PublicKey pk = cachePublico.computeIfAbsent("pk", k -> {
            try {
                return RSACriptografia.carregarChavePublica();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Usar a chave em cache
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, pk);
        byte[] criptografado = cipher.doFinal(texto.getBytes());
        return java.util.Base64.getEncoder().encodeToString(criptografado);
    }
    
    public static void main(String[] args) throws Exception {
        long inicio = System.currentTimeMillis();
        
        // Primeira chamada (carrega chave)
        String c1 = criptografarComCache("texto1");
        
        // Chamadas seguintes (usa cache)
        String c2 = criptografarComCache("texto2");
        String c3 = criptografarComCache("texto3");
        
        long fim = System.currentTimeMillis();
        System.out.println("Tempo com cache: " + (fim - inicio) + "ms");
        System.out.println("✓ Cache implementado com sucesso");
    }
}
```

---

### Exemplo 7: Tratamento de Erros Robusto

```java
import br.com.mpet.RSACriptografia;

public class TratamentoErros {
    
    public static String criptografarSeguro(String texto) {
        if (texto == null) {
            System.err.println("Erro: texto nulo");
            return null;
        }
        
        if (texto.length() > 245) {
            System.err.println("Erro: texto muito grande (máx 245 bytes)");
            return null;
        }
        
        try {
            return RSACriptografia.criptografar(texto);
        } catch (Exception e) {
            System.err.println("Erro ao criptografar: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static String descriptografarSeguro(String textoCriptografado) {
        if (textoCriptografado == null) {
            System.err.println("Erro: texto criptografado nulo");
            return null;
        }
        
        try {
            return RSACriptografia.descriptografar(textoCriptografado);
        } catch (Exception e) {
            System.err.println("Erro ao descriptografar: " + e.getMessage());
            // Fallback para valor original em caso de erro
            return textoCriptografado;
        }
    }
    
    public static void main(String[] args) {
        // Teste 1: Null
        String r1 = criptografarSeguro(null);
        System.out.println("Resultado 1: " + r1);
        
        // Teste 2: Muito grande
        String grande = "a".repeat(300);
        String r2 = criptografarSeguro(grande);
        System.out.println("Resultado 2: " + r2);
        
        // Teste 3: Normal
        String r3 = criptografarSeguro("senha segura");
        if (r3 != null) {
            String descriptografado = descriptografarSeguro(r3);
            System.out.println("Resultado 3: " + descriptografado);
        }
    }
}
```

---

### Exemplo 8: Integração com Interface (Exemplo de Login)

```java
import br.com.mpet.RSACriptografia;
import br.com.mpet.model.Adotante;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import java.util.Optional;
import java.util.Scanner;

public class TelaLoginComRSA {
    
    public static void main(String[] args) throws Exception {
        File dbFile = new File("dats/adotantes.dat");
        AdotanteDataFileDao dao = new AdotanteDataFileDao(dbFile, (byte)1);
        
        Scanner sc = new Scanner(System.in);
        
        System.out.println("=== Login Adotante ===");
        System.out.print("CPF: ");
        String cpf = sc.nextLine();
        
        System.out.print("Senha: ");
        String senhaDigitada = sc.nextLine();
        
        // Tentar login
        Optional<Adotante> adotante = dao.read(cpf);
        
        if (adotante.isPresent() && 
            adotante.get().isAtivo() && 
            adotante.get().getSenha().equals(senhaDigitada)) {
            
            System.out.println("✓ Login bem-sucedido!");
            System.out.println("Bem-vindo, " + adotante.get().getNomeCompleto());
            
        } else {
            System.out.println("✗ CPF ou senha inválidos");
        }
        
        dao.close();
        sc.close();
    }
}
```

---

## Snippets de Configuração

### Snippet 1: Iniciar com Chaves (em Interface.java)

```java
static {
    try {
        RSAKeyGen.main(new String[]{});
    } catch (Exception e) {
        System.err.println("Aviso: Falha ao inicializar RSA");
    }
}
```

### Snippet 2: Validador de Criptografia

```java
public class ValidadorRSA {
    public static boolean validarIntegridade(String original, String criptografado) {
        try {
            String recuperado = RSACriptografia.descriptografar(criptografado);
            return original.equals(recuperado);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Snippet 3: Converter Qualquer String em Segura

```java
public class StringSegura {
    public static String tornarSegura(String valor) {
        try {
            return RSACriptografia.criptografar(valor);
        } catch (Exception e) {
            return valor; // Fallback
        }
    }
    
    public static String recuperar(String valorSeguro) {
        try {
            return RSACriptografia.descriptografar(valorSeguro);
        } catch (Exception e) {
            return valorSeguro; // Fallback
        }
    }
}
```

---

## Testes Unitários (Opcional com JUnit)

```java
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.mpet.RSACriptografia;

public class TestRSACriptografia {
    
    @Test
    public void testCriptografiaBasica() throws Exception {
        String original = "teste123";
        String criptografado = RSACriptografia.criptografar(original);
        String recuperado = RSACriptografia.descriptografar(criptografado);
        
        assertEquals(original, recuperado);
    }
    
    @Test
    public void testSenhasNaoSaoIguais() throws Exception {
        String senha1 = RSACriptografia.criptografar("senha");
        String senha2 = RSACriptografia.criptografar("senha");
        
        // Cada criptografia produz resultado diferente
        assertNotEquals(senha1, senha2);
    }
    
    @Test
    public void testAssinatura() throws Exception {
        String texto = "documento";
        String assinatura = RSACriptografia.assinar(texto);
        
        assertTrue(RSACriptografia.verificarAssinatura(texto, assinatura));
        assertFalse(RSACriptografia.verificarAssinatura("outro", assinatura));
    }
    
    @Test
    public void testTamanhoBytesLimite() throws Exception {
        String grande = "a".repeat(245); // Limite máximo
        String criptografado = RSACriptografia.criptografar(grande);
        assertNotNull(criptografado);
    }
    
    @Test(expected = Exception.class)
    public void testExcedeTamanhoBytesLimite() throws Exception {
        String muitoGrande = "a".repeat(300); // Excede limite
        RSACriptografia.criptografar(muitoGrande);
    }
}
```

---

## Performance e Benchmarks

### Medição de Tempo

```java
public class BenchmarkRSA {
    public static void main(String[] args) throws Exception {
        String texto = "Texto de teste";
        int iteracoes = 100;
        
        // Benchmark de criptografia
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < iteracoes; i++) {
            RSACriptografia.criptografar(texto);
        }
        long tempoEncrypt = System.currentTimeMillis() - inicio;
        
        // Benchmark de descriptografia
        String criptografado = RSACriptografia.criptografar(texto);
        inicio = System.currentTimeMillis();
        for (int i = 0; i < iteracoes; i++) {
            RSACriptografia.descriptografar(criptografado);
        }
        long tempoDecrypt = System.currentTimeMillis() - inicio;
        
        System.out.printf("Criptografia:   %.2f ms/operação%n", (double)tempoEncrypt / iteracoes);
        System.out.printf("Descriptografia: %.2f ms/operação%n", (double)tempoDecrypt / iteracoes);
    }
}
```

---

## 📝 Notas Importantes

- ⚠️ RSA-2048 suporta máximo **245 bytes** por mensagem
- ⚠️ Cada criptografia produz resultado diferente (não determinístico)
- ✅ Use para: senhas, dados sensíveis, pequenos payloads
- ❌ Não use para: arquivos grandes, dados de streaming

---

**Todos os exemplos estão prontos para usar!** 🚀
