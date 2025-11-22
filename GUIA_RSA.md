# Guia de Criptografia RSA

## Visão Geral
Este projeto implementa criptografia RSA com 2048 bits para proteção de dados sensíveis. O sistema utiliza duas classes principais:

1. **RSAKeyGen** - Gera e salva o par de chaves RSA
2. **RSACriptografia** - Fornece métodos para criptografar, descriptografar e assinar digitalmente

## Estrutura de Arquivos

```
Codigo/
├── src/main/java/br/com/mpet/
│   ├── RSAKeyGen.java           # Gerador de chaves
│   └── RSACriptografia.java     # Operações de criptografia
└── keys/                        # Diretório de chaves (criado automaticamente)
    ├── public_key.pem          # Chave pública
    └── private_key.pem         # Chave privada (confidencial)
```

## Instruções de Uso

### 1. Gerar Chaves RSA

Na primeira execução, gere o par de chaves:

```bash
cd Codigo
javac -d target/classes src/main/java/br/com/mpet/RSAKeyGen.java
java -cp target/classes br.com.mpet.RSAKeyGen
```

Saída esperada:
```
✓ Chaves RSA geradas com sucesso!
  Chave pública: keys/public_key.pem
  Chave privada: keys/private_key.pem
```

### 2. Usar a Criptografia no Seu Código

#### Criptografar um texto:
```java
String textoSensivel = "Senha do usuário";
String textoCriptografado = RSACriptografia.criptografar(textoSensivel);
System.out.println("Criptografado: " + textoCriptografado);
```

#### Descriptografar um texto:
```java
String textoRecuperado = RSACriptografia.descriptografar(textoCriptografado);
System.out.println("Original: " + textoRecuperado);
```

#### Trabalhar com bytes:
```java
byte[] dados = "Dados binários".getBytes();
byte[] dadosCriptografados = RSACriptografia.criptografarBytes(dados);
byte[] dadosOriginais = RSACriptografia.descriptografarBytes(dadosCriptografados);
```

#### Assinar digitalmente:
```java
String mensagem = "Documento importante";
String assinatura = RSACriptografia.assinar(mensagem);
boolean valida = RSACriptografia.verificarAssinatura(mensagem, assinatura);
System.out.println("Assinatura válida: " + valida);
```

### 3. Executar Testes

Para testar toda a funcionalidade RSA:

```bash
java -cp target/classes br.com.mpet.RSACriptografia
```

## Métodos Disponíveis

### Criptografia de Texto
- `String criptografar(String texto)` - Criptografa um texto
- `String descriptografar(String textoCriptografado)` - Descriptografa um texto

### Criptografia de Bytes
- `byte[] criptografarBytes(byte[] dados)` - Criptografa dados binários
- `byte[] descriptografarBytes(byte[] dadosCriptografados)` - Descriptografa dados binários

### Assinatura Digital
- `String assinar(String texto)` - Cria uma assinatura SHA256withRSA
- `boolean verificarAssinatura(String texto, String assinatura)` - Verifica a autenticidade

### Carregamento de Chaves
- `PublicKey carregarChavePublica()` - Carrega a chave pública do arquivo PEM
- `PrivateKey carregarChavePrivada()` - Carrega a chave privada do arquivo PEM

## Segurança

⚠️ **Importante:**
- A chave privada (`keys/private_key.pem`) é **confidencial** e nunca deve ser compartilhada
- Garanta que o diretório `keys/` tenha permissões restritas
- Em produção, use um gerenciador de chaves dedicado (ex: AWS KMS, Azure Key Vault)
- Nunca versione a chave privada no Git

## Características

✅ **RSA 2048 bits** - Nível de segurança adequado para a maioria das aplicações  
✅ **Assinatura Digital** - SHA256withRSA para autenticação  
✅ **Base64 Encoding** - Textos criptografados em Base64 para facilitar transmissão  
✅ **Tratamento de Exceções** - Propagação adequada de exceções de segurança  

## Exemplo Completo de Integração

```java
import br.com.mpet.RSACriptografia;

public class ExemploRSA {
    public static void main(String[] args) throws Exception {
        // Senhas de usuários a proteger
        String senhaUsuario = "senha123";
        
        // Criptografar a senha
        String senhaCriptografada = RSACriptografia.criptografar(senhaUsuario);
        System.out.println("Senha criptografada salva no banco de dados");
        
        // Verificar a senha (descriptografar e comparar)
        String entrada = "senha123";
        String entradadaCriptografada = RSACriptografia.criptografar(entrada);
        String entradaDescriptografada = RSACriptografia.descriptografar(entradadaCriptografada);
        
        if (entradaDescriptografada.equals(entrada)) {
            System.out.println("✓ Senha válida!");
        } else {
            System.out.println("✗ Senha inválida!");
        }
    }
}
```

## Troubleshooting

### Erro: "keys/ not found"
**Solução:** Execute primeiro `RSAKeyGen` para gerar as chaves.

### Erro: "javax.crypto.BadPaddingException"
**Solução:** O texto está corrompido ou foi descriptografado com a chave errada.

### Erro: "java.security.InvalidKeyException: key size too large"
**Solução:** O tamanho da mensagem excede 245 bytes. Use RSA apenas para dados pequenos ou considere usar híbrido RSA+AES para dados grandes.
