package br.com.mpet;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class HuffmanNode implements Comparable<HuffmanNode> {
    byte b;
    int frequencia;
    HuffmanNode esquerdo, direito;

    public HuffmanNode(byte b, int f, HuffmanNode esquerdo, HuffmanNode direito) {
        this.b = b;
        this.frequencia = f;
        this.esquerdo = esquerdo;
        this.direito = direito;
    }

    public boolean isFolha() {
        return this.esquerdo == null && this.direito == null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return this.frequencia - o.frequencia;
    }
}

public class Huffman {

    public static byte[] codifica(byte[] dados) {
        if (dados == null || dados.length == 0) {
            return new byte[0];
        }

        Map<Byte, Integer> mapaDeFrequencias = new HashMap<>();
        for (byte b : dados) {
            mapaDeFrequencias.put(b, mapaDeFrequencias.getOrDefault(b, 0) + 1);
        }

        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : mapaDeFrequencias.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue(), null, null));
        }

        while (pq.size() > 1) {
            HuffmanNode esquerdo = pq.poll();
            HuffmanNode direito = pq.poll();
            HuffmanNode pai = new HuffmanNode((byte) 0, esquerdo.frequencia + direito.frequencia, esquerdo, direito);
            pq.add(pai);
        }

        HuffmanNode raiz = pq.poll();
        Map<Byte, String> codigos = new HashMap<>();
        constroiCodigos(raiz, "", codigos);

        VetorDeBits vdb = new VetorDeBits();
        int bitIndex = 0;
        for (byte b : dados) {
            String codigo = codigos.get(b);
            for (char c : codigo.toCharArray()) {
                if (c == '1') {
                    vdb.set(bitIndex);
                } else {
                    vdb.clear(bitIndex);
                }
                bitIndex++;
            }
        }

        // A implementação da descompressão precisaria da árvore ou da tabela de frequência.
        // Para este exercício, focamos na compressão em um arquivo.
        // O retorno é o array de bytes comprimido.
        return vdb.toByteArray();
    }

    private static void constroiCodigos(HuffmanNode no, String codigo, Map<Byte, String> codigos) {
        if (no == null) {
            return;
        }
        if (no.isFolha()) {
            codigos.put(no.b, codigo);
            return;
        }
        constroiCodigos(no.esquerdo, codigo + "0", codigos);
        constroiCodigos(no.direito, codigo + "1", codigos);
    }

    // A decodificação não foi solicitada, mas seria necessária para um sistema completo de backup/restore.
    // public static byte[] decodifica(byte[] dadosComprimidos, HuffmanNode raiz) { ... }

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