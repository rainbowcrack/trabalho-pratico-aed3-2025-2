package br.com.mpet;

import java.util.BitSet;

/**
 * A classe {@code VetorDeBits} encapsula um {@link java.util.BitSet} para
 * manipular uma sequência de bits de tamanho dinâmico.
 * Fornece métodos para definir, limpar, obter bits e converter
 * de/para array de bytes.
 */
public class VetorDeBits {

    private BitSet bitset;
    private int tamanho;

    /**
     * Constrói um VetorDeBits vazio com capacidade inicial.
     */
    public VetorDeBits() {
        this.bitset = new BitSet();
        this.tamanho = 0;
    }

    /**
     * Constrói um VetorDeBits com um tamanho inicial específico.
     * @param nbits O número de bits inicial.
     */
    public VetorDeBits(int nbits) {
        this.bitset = new BitSet(nbits);
        this.tamanho = nbits;
    }

    /**
     * Constrói um VetorDeBits a partir de um array de bytes.
     * @param bytes O array de bytes para inicializar o vetor de bits.
     */
    public VetorDeBits(byte[] bytes) {
        this.bitset = BitSet.valueOf(bytes);
        this.tamanho = bytes.length * 8;
    }

    /**
     * Define o bit no índice especificado para {@code true}.
     * @param index O índice do bit a ser definido.
     */
    public void set(int index) {
        bitset.set(index);
        if (index >= tamanho) {
            tamanho = index + 1;
        }
    }

    /**
     * Define o bit no índice especificado para {@code false}.
     * @param index O índice do bit a ser limpo.
     */
    public void clear(int index) {
        bitset.clear(index);
        if (index >= tamanho) {
            tamanho = index + 1;
        }
    }

    /**
     * Retorna o valor do bit no índice especificado.
     * @param index O índice do bit.
     * @return {@code true} se o bit estiver definido, {@code false} caso contrário.
     */
    public boolean get(int index) {
        return bitset.get(index);
    }

    /**
     * Retorna o número de bits neste vetor de bits.
     * @return O tamanho lógico deste vetor de bits.
     */
    public int length() {
        return tamanho;
    }

    /**
     * Converte o vetor de bits para um array de bytes.
     * @return Um array de bytes representando os bits.
     */
    public byte[] toByteArray() {
        return bitset.toByteArray();
    }

    /**
     * Retorna uma representação em String do vetor de bits (ex: "011010").
     * @return A string de '0's e '1's.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(bitset.get(i) ? '1' : '0');
        }
        return sb.toString();
    }
}