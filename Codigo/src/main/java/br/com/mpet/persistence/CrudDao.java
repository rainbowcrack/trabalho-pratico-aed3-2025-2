package br.com.mpet.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Interface genérica de operações CRUD para entidades persistidas em arquivo binário.
 * K = tipo da chave primária
 * T = tipo da entidade
 */
public interface CrudDao<T, K> extends Closeable {

    /** Cria entidade e retorna a entidade com ID preenchido (se gerado) */
    T create(T entity) throws IOException;

    /** Lê entidade pelo id (apenas se ativa / não tombstoned) */
    Optional<T> read(K id) throws IOException;

    /** Atualiza entidade existente; retorna true se sucesso */
    boolean update(T entity) throws IOException;

    /** Marca entidade como removida (tombstone) */
    boolean delete(K id) throws IOException;

    /** Lista todas as entidades ativas (custo O(n) no arquivo) */
    List<T> listAllActive() throws IOException;

    /** Reconstrói índice se arquivo estiver consistente mas índice vazio */
    void rebuildIfEmpty() throws IOException;

    /** Compacta removendo registros tombstoned (gera novo arquivo) */
    void vacuum() throws IOException;

    @Override
    void close() throws IOException;
}


