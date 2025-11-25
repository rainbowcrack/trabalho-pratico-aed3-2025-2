package br.com.mpet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    /**
     * Codifica dados com Huffman e inclui a tabela de frequências para descompressão
     */
    public static byte[] codifica(byte[] dados) {
        if (dados == null || dados.length == 0) {
            return new byte[0];
        }

        // 1. Calcular frequências
        Map<Byte, Integer> mapaDeFrequencias = new HashMap<>();
        for (byte b : dados) {
            mapaDeFrequencias.put(b, mapaDeFrequencias.getOrDefault(b, 0) + 1);
        }

        // 2. Construir árvore Huffman
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
        
        // 3. Gerar códigos
        Map<Byte, String> codigos = new HashMap<>();
        constroiCodigos(raiz, "", codigos);

        // 4. Codificar dados
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
        
        byte[] dadosCodificados = vdb.toByteArray();
        
        // 5. Serializar: [tamanhoOriginal(4)][numFreq(4)][freq1(byte+int)][freq2]...[tamCodificado(4)][dadosCodificados]
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Tamanho original
            baos.write(ByteBuffer.allocate(4).putInt(dados.length).array());
            
            // Número de símbolos únicos
            baos.write(ByteBuffer.allocate(4).putInt(mapaDeFrequencias.size()).array());
            
            // Tabela de frequências
            for (Map.Entry<Byte, Integer> entry : mapaDeFrequencias.entrySet()) {
                baos.write(entry.getKey()); // 1 byte
                baos.write(ByteBuffer.allocate(4).putInt(entry.getValue()).array()); // 4 bytes
            }
            
            // Tamanho dos dados codificados
            baos.write(ByteBuffer.allocate(4).putInt(dadosCodificados.length).array());
            
            // Dados codificados
            baos.write(dadosCodificados);
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Erro ao serializar dados Huffman", e);
        }
    }

    /**
     * Decodifica dados comprimidos com Huffman
     */
    public static byte[] decodifica(byte[] dadosComprimidos) {
        if (dadosComprimidos == null || dadosComprimidos.length == 0) {
            return new byte[0];
        }
        
        try {
            int pos = 0;
            
            // 1. Ler tamanho original
            int tamanhoOriginal = ByteBuffer.wrap(dadosComprimidos, pos, 4).getInt();
            pos += 4;
            
            // 2. Ler número de símbolos
            int numSimbolos = ByteBuffer.wrap(dadosComprimidos, pos, 4).getInt();
            pos += 4;
            
            // 3. Ler tabela de frequências
            Map<Byte, Integer> mapaDeFrequencias = new HashMap<>();
            for (int i = 0; i < numSimbolos; i++) {
                byte simbolo = dadosComprimidos[pos++];
                int freq = ByteBuffer.wrap(dadosComprimidos, pos, 4).getInt();
                pos += 4;
                mapaDeFrequencias.put(simbolo, freq);
            }
            
            // 4. Reconstruir árvore Huffman
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
            
            // 5. Ler tamanho dos dados codificados
            int tamCodificado = ByteBuffer.wrap(dadosComprimidos, pos, 4).getInt();
            pos += 4;
            
            // 6. Extrair dados codificados
            byte[] dadosCodificados = new byte[tamCodificado];
            System.arraycopy(dadosComprimidos, pos, dadosCodificados, 0, tamCodificado);
            
            // 7. Decodificar usando a árvore
            VetorDeBits vdb = new VetorDeBits(dadosCodificados);
            byte[] resultado = new byte[tamanhoOriginal];
            int resultIndex = 0;
            HuffmanNode noAtual = raiz;
            
            for (int bitIndex = 0; bitIndex < vdb.length() && resultIndex < tamanhoOriginal; bitIndex++) {
                if (noAtual.isFolha()) {
                    resultado[resultIndex++] = noAtual.b;
                    noAtual = raiz;
                }
                
                if (resultIndex >= tamanhoOriginal) break;
                
                if (vdb.get(bitIndex)) {
                    noAtual = noAtual.direito;
                } else {
                    noAtual = noAtual.esquerdo;
                }
                
                if (noAtual == null) break;
            }
            
            // Último símbolo
            if (noAtual != null && noAtual.isFolha() && resultIndex < tamanhoOriginal) {
                resultado[resultIndex] = noAtual.b;
            }
            
            return resultado;
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao decodificar dados Huffman: " + e.getMessage(), e);
        }
    }

    private static void constroiCodigos(HuffmanNode no, String codigo, Map<Byte, String> codigos) {
        if (no == null) {
            return;
        }
        if (no.isFolha()) {
            codigos.put(no.b, codigo.isEmpty() ? "0" : codigo);
            return;
        }
        constroiCodigos(no.esquerdo, codigo + "0", codigos);
        constroiCodigos(no.direito, codigo + "1", codigos);
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