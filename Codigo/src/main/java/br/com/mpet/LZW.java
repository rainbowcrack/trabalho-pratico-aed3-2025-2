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

    /**
     * Decodifica dados comprimidos pelo algoritmo LZW
     */
    public static byte[] decodifica(byte[] dadosComprimidos) {
        if (dadosComprimidos == null || dadosComprimidos.length == 0) {
            return new byte[0];
        }

        // Converte bytes para lista de índices
        ArrayList<Integer> indices = byteArrayParaIndices(dadosComprimidos);
        if (indices.isEmpty()) {
            return new byte[0];
        }

        // Inicializa o dicionário com todos os bytes possíveis
        int dictSize = 256;
        Map<Integer, String> dicionario = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dicionario.put(i, "" + (char) i);
        }

        // Lê o primeiro código
        int codigo = indices.get(0);
        String w = dicionario.get(codigo);
        ArrayList<Byte> resultado = new ArrayList<>();
        
        // Adiciona os bytes da primeira entrada
        for (char c : w.toCharArray()) {
            resultado.add((byte) c);
        }

        // Processa os códigos restantes
        for (int i = 1; i < indices.size(); i++) {
            int k = indices.get(i);
            String entrada;
            
            if (dicionario.containsKey(k)) {
                entrada = dicionario.get(k);
            } else if (k == dictSize) {
                // Caso especial: código ainda não está no dicionário
                entrada = w + w.charAt(0);
            } else {
                throw new IllegalArgumentException("Código LZW inválido: " + k);
            }

            // Adiciona bytes da entrada ao resultado
            for (char c : entrada.toCharArray()) {
                resultado.add((byte) c);
            }

            // Adiciona nova entrada ao dicionário
            if (dictSize < MAX_DICIONARIO_SIZE) {
                dicionario.put(dictSize++, w + entrada.charAt(0));
            }

            w = entrada;
        }

        // Converte ArrayList<Byte> para byte[]
        byte[] saida = new byte[resultado.size()];
        for (int i = 0; i < resultado.size(); i++) {
            saida[i] = resultado.get(i);
        }
        return saida;
    }

    private static ArrayList<Integer> byteArrayParaIndices(byte[] bytes) {
        VetorDeBits vdb = new VetorDeBits(bytes);
        ArrayList<Integer> indices = new ArrayList<>();
        int totalBits = bytes.length * 8;
        
        for (int bitIndex = 0; bitIndex + BITS_POR_INDICE <= totalBits; bitIndex += BITS_POR_INDICE) {
            int indice = 0;
            for (int i = 0; i < BITS_POR_INDICE; i++) {
                if (vdb.get(bitIndex + i)) {
                    indice |= (1 << (BITS_POR_INDICE - 1 - i));
                }
            }
            indices.add(indice);
        }
        return indices;
    }

    public static void main(String[] args) {
        String frase = "O sabiá não sabia que o sábio sabia que o sabiá não sabia assobiar.";
        System.out.println("Frase original: " + frase);
        byte[] dadosOriginais = frase.getBytes();

        byte[] dadosComprimidos = codifica(dadosOriginais);

        System.out.println("\nTamanho original: " + dadosOriginais.length + " bytes");
        System.out.println("Tamanho compactado: " + dadosComprimidos.length + " bytes");

        float eficiencia = (1 - (float) dadosComprimidos.length / dadosOriginais.length) * 100;
        System.out.printf("Eficiência: %.2f%%\n", eficiencia);
        
        // Testar descompressão
        System.out.println("\n--- Teste de Descompressão ---");
        byte[] dadosDescomprimidos = decodifica(dadosComprimidos);
        String fraseRecuperada = new String(dadosDescomprimidos);
        System.out.println("Frase recuperada: " + fraseRecuperada);
        System.out.println("Descompressão " + (frase.equals(fraseRecuperada) ? "✓ OK!" : "✗ FALHOU!"));
    }
}