package br.com.mpet.persistence.index;

import java.io.IOException;

/**
 * Garante que o objeto tenha um ID (para hashing), um tamanho fixo e possa ser
 * deserializado para um array de bytes
 */

 /*
REGISTRO HASH EXTENSÍVEL

Esta interface apresenta os métodos que os objetos
a serem incluídos na tabela hash extensível devem 
conter.

Implementado pelo Prof. Marcos Kutova (modificado MPET)
v1.1 - 2021
*/
public interface RegistroHash {

    int getId();

    void setId(int id);

    short size();

    byte[] toByteArray() throws IOException;

    void fromByteArray(byte[] ba) throws IOException;
}