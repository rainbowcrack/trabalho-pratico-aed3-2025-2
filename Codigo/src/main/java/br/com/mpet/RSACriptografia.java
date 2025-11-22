package br.com.mpet;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Classe para criptografia e descriptografia usando RSA.
 * Utiliza chaves geradas pelo RSAKeyGen.
 */
public class RSACriptografia {
    private static final String ALGORITHM = "RSA";
    private static final String PUBLIC_KEY_FILE = "keys/public_key.pem";
    private static final String PRIVATE_KEY_FILE = "keys/private_key.pem";

    /**
     * Carrega a chave pública do arquivo PEM
     */
    public static PublicKey carregarChavePublica() throws Exception {
        String keyContent = new String(Files.readAllBytes(Paths.get(PUBLIC_KEY_FILE)));
        keyContent = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Carrega a chave privada do arquivo PEM
     */
    public static PrivateKey carregarChavePrivada() throws Exception {
        String keyContent = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY_FILE)));
        keyContent = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Criptografa um texto usando a chave pública
     */
    public static String criptografar(String texto) throws Exception {
        PublicKey publicKey = carregarChavePublica();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] textoByte = texto.getBytes();
        byte[] textoCriptografado = cipher.doFinal(textoByte);

        return Base64.getEncoder().encodeToString(textoCriptografado);
    }

    /**
     * Descriptografa um texto criptografado usando a chave privada
     */
    public static String descriptografar(String textoCriptografado) throws Exception {
        PrivateKey privateKey = carregarChavePrivada();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] textoCriptografadoByte = Base64.getDecoder().decode(textoCriptografado);
        byte[] textoDescriptografado = cipher.doFinal(textoCriptografadoByte);

        return new String(textoDescriptografado);
    }

    /**
     * Criptografa bytes usando a chave pública
     */
    public static byte[] criptografarBytes(byte[] dados) throws Exception {
        PublicKey publicKey = carregarChavePublica();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(dados);
    }

    /**
     * Descriptografa bytes usando a chave privada
     */
    public static byte[] descriptografarBytes(byte[] dadosCriptografados) throws Exception {
        PrivateKey privateKey = carregarChavePrivada();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(dadosCriptografados);
    }

    /**
     * Cria uma assinatura digital usando a chave privada
     */
    public static String assinar(String texto) throws Exception {
        PrivateKey privateKey = carregarChavePrivada();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(texto.getBytes());

        byte[] assinatura = signature.sign();
        return Base64.getEncoder().encodeToString(assinatura);
    }

    /**
     * Verifica uma assinatura digital usando a chave pública
     */
    public static boolean verificarAssinatura(String texto, String assinatura) throws Exception {
        PublicKey publicKey = carregarChavePublica();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(texto.getBytes());

        byte[] assinaturaByte = Base64.getDecoder().decode(assinatura);
        return signature.verify(assinaturaByte);
    }

    // Exemplo de uso
    public static void main(String[] args) {
        try {
            System.out.println("=== Teste de Criptografia RSA ===\n");

            // Teste 1: Criptografia e Descriptografia de texto
            String textoOriginal = "Informação sensível";
            System.out.println("Texto original: " + textoOriginal);

            String textoCriptografado = criptografar(textoOriginal);
            System.out.println("Texto criptografado: " + textoCriptografado);

            String textoDescriptografado = descriptografar(textoCriptografado);
            System.out.println("Texto descriptografado: " + textoDescriptografado);
            System.out.println();

            // Teste 2: Assinatura digital
            String mensagem = "Mensagem importante";
            System.out.println("Mensagem: " + mensagem);

            String assinatura = assinar(mensagem);
            System.out.println("Assinatura: " + assinatura);

            boolean valida = verificarAssinatura(mensagem, assinatura);
            System.out.println("Assinatura válida: " + valida);

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
