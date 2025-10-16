package br.com.mpet.persistence.index;

import java.io.IOException;

/**
 * REGISTRO ÁRVORE B+
 *
 * Esta interface apresenta os métodos que os objetos
 * a serem incluídos na árvore B+ devem
 * conter.
 *
 * Modificação da implementação feita pelo Prof. Marcos Kutova
 * v2.0 - 2025
 */
public interface RegistroArvoreBMais<T> {
    public short size(); // tamanho FIXO do registro
    public byte[] toByteArray() throws IOException; // representação do elemento em um vetor de bytes
    public void fromByteArray(byte[] ba) throws IOException; // vetor de bytes a ser usado na construção do elemento
    public int compareTo(T obj); // compara dois elementos
    public T clone(); // clonagem de objetos
}