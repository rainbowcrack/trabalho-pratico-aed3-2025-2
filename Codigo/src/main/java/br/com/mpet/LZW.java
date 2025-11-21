package br.com.mpet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A classe {@code LZW} codifica dados usando o algoritmo LZW.
 * A codificação opera sobre bytes.
 *
 * @author Marcos Kutova (adaptado)
 *         PUC Minas
 */
public class LZW {

    public static final int BITS_POR_INDICE = 12; // Mínimo de 9 bits por índice (512 itens no dicionário)
    private static final int MAX_DICIONARIO_SIZE = (1 << BITS_POR_INDICE);

    public static byte[] codifica(byte[] dados) {
        if (dados == null || dados.length == 0) {
            return new byte[0];
        }

        // Inicializa o dicionário com todos os bytes possíveis
        int dictSize = 256;
        Map<String, Integer> dicionario = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dicionario.put("" + (char) i, i);
        }

        String w = "";
        ArrayList<Integer> resultado = new ArrayList<>();
        for (byte b : dados) {
            char c = (char) (b & 0xFF); // Trata byte como unsigned
            String wc = w + c;
            if (dicionario.containsKey(wc)) {
                w = wc;
            } else {
                resultado.add(dicionario.get(w));
                // Adiciona wc ao dicionário
                if (dictSize < MAX_DICIONARIO_SIZE) {
                    dicionario.put(wc, dictSize++);
                }
                w = "" + c;
            }
        }

        // Escreve o último código
        if (!w.equals("")) {
            resultado.add(dicionario.get(w));
        }

        return indicesParaByteArray(resultado);
    }

    private static byte[] indicesParaByteArray(ArrayList<Integer> indices) {
        VetorDeBits vdb = new VetorDeBits(indices.size() * BITS_POR_INDICE);
        int bitIndex = 0;
        for (int indice : indices) {
            for (int i = BITS_POR_INDICE - 1; i >= 0; i--) {
                if (((indice >> i) & 1) == 1) {
                    vdb.set(bitIndex);
                } else {
                    vdb.clear(bitIndex);
                }
                bitIndex++;
            }
        }
        return vdb.toByteArray();
    }

    // A decodificação não foi solicitada, mas seria necessária para um sistema completo de backup/restore.
    // public static byte[] decodifica(byte[] dadosComprimidos) { ... }

    public static void main(String[] args) {
        String frase = "O sabiá não sabia que o sábio sabia que o sabiá não sabia assobiar.";
        System.out.println("Frase original: " + frase);
        byte[] dadosOriginais = frase.getBytes();

        byte[] dadosComprimidos = codifica(dadosOriginais);

        System.out.println("\nTamanho original: " + dadosOriginais.length + " bytes");
        System.out.println("Tamanho compactado: " + dadosComprimidos.length + " bytes");

        float eficiencia = (1 - (float) dadosComprimidos.length / dadosOriginais.length) * 100;
        System.out.printf("Eficiência: %.2f%%\n", eficiencia);
    }
}