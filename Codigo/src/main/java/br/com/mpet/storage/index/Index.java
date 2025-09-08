package br.com.mpet.storage.index;

import java.io.IOException;
import java.util.Optional;

public interface Index<K> extends AutoCloseable {
    void put(K key, long address) throws IOException;
    Optional<Long> get(K key) throws IOException;
    void remove(K key) throws IOException;
    void close() throws IOException;
}
