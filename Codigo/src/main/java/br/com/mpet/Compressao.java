package br.com.mpet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressao {

    private static final String PASTA_DADOS = "dats";
    private static final String ARQUIVO_BACKUP = "backup.zip";

    public static void comprimir(int versao) throws IOException {
        File pastaDados = new File(PASTA_DADOS);
        if (!pastaDados.exists() || !pastaDados.isDirectory()) {
            System.out.println("Pasta de dados '" + PASTA_DADOS + "' não encontrada.");
            return;
        }

        File[] arquivos = pastaDados.listFiles();
        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum arquivo de dados para comprimir.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(ARQUIVO_BACKUP);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File arquivo : arquivos) {
                if (arquivo.isFile()) {
                    System.out.println("Comprimindo arquivo: " + arquivo.getName());

                    // 1. Ler o conteúdo do arquivo
                    byte[] dadosOriginais;
                    try (FileInputStream fis = new FileInputStream(arquivo)) {
                        dadosOriginais = fis.readAllBytes();
                    }

                    // 2. Comprimir os dados
                    byte[] dadosComprimidos;
                    String nomeAlgoritmo;
                    if (versao == 1) { // Huffman
                        dadosComprimidos = Huffman.codifica(dadosOriginais);
                        nomeAlgoritmo = "Huffman";
                    } else { // LZW
                        dadosComprimidos = LZW.codifica(dadosOriginais);
                        nomeAlgoritmo = "LZW";
                    }

                    // 3. Adicionar ao ZIP
                    ZipEntry zipEntry = new ZipEntry(arquivo.getName() + ".compressed");
                    zos.putNextEntry(zipEntry);
                    zos.write(dadosComprimidos);
                    zos.closeEntry();

                    System.out.printf(" -> Tamanho original: %d bytes\n", dadosOriginais.length);
                    System.out.printf(" -> Tamanho comprimido (%s): %d bytes\n", nomeAlgoritmo, dadosComprimidos.length);
                    if (dadosOriginais.length > 0) {
                        float eficiencia = (1 - (float) dadosComprimidos.length / dadosOriginais.length) * 100;
                        System.out.printf(" -> Eficiência: %.2f%%\n", eficiencia);
                    }
                }
            }
            System.out.println("\nBackup concluído com sucesso! Arquivo gerado: " + ARQUIVO_BACKUP);

        } catch (IOException e) {
            System.err.println("Ocorreu um erro durante a compressão: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lança para que a interface possa tratar se necessário
        }
    }

    // A descompressão não foi solicitada, mas um método correspondente seria necessário aqui.
    // public static void descomprimir() throws IOException { ... }
}