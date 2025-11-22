package br.com.mpet;

import java.nio.file.*;
import java.security.*;
import java.util.Base64;

public class RSAKeyGen {
    private static final String KEY_DIR = "keys";
    private static final String PUBLIC_KEY_FILE = "keys/public_key.pem";
    private static final String PRIVATE_KEY_FILE = "keys/private_key.pem";

    public static void main(String[] args) throws Exception {
        // Criar diretório se não existir
        Files.createDirectories(Paths.get(KEY_DIR));

        // Gera um par de chaves RSA com 2048 bits
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Salva a chave pública
        byte[] publicKeyEncoded = keyPair.getPublic().getEncoded();
        String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(publicKeyEncoded)
                + "\n-----END PUBLIC KEY-----";
        Files.write(Paths.get(PUBLIC_KEY_FILE), publicKeyPEM.getBytes());

        // Salva a chave privada
        byte[] privateKeyEncoded = keyPair.getPrivate().getEncoded();
        String privateKeyPEM = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(privateKeyEncoded)
                + "\n-----END PRIVATE KEY-----";
        Files.write(Paths.get(PRIVATE_KEY_FILE), privateKeyPEM.getBytes());

        System.out.println("✓ Chaves RSA geradas com sucesso!");
        System.out.println("  Chave pública: " + PUBLIC_KEY_FILE);
        System.out.println("  Chave privada: " + PRIVATE_KEY_FILE);
    }
}