package br.com.mpet.persistence.index;

import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.TreeMap;

/**
 * Índice do tipo "B+ Tree" simplificado para mapear chave inteira -> offset de registro (long).
 *
 * IMPORTANTE: Esta implementação mantém todos os pares (k -> offset) em memória (TreeMap)
 * e persiste a lista ordenada linearmente no arquivo .idx. A estrutura de cabeçalho
 * segue o helper de Árvore B+ ({@link FileHeaderHelper.BPlusTreeHeader}), o que facilita
 * evoluir para nós e divisões (splits) no futuro. Para o trabalho prático atual,
 * este índice já oferece consultas O(log n) em memória e persistência simples.
 *
 * Layout do arquivo .idx:
 *   - Cabeçalho B+ de 128 bytes (ver FileHeaderHelper)
 *   - Área de entradas: sequência de tuplas (int key, long offset) ordenadas por key
 *
 * Métodos principais:
 *   - put(key, offset): insere/atualiza a chave, regravando a área de entradas
 *   - get(key): retorna offset ou null
 *   - remove(key): remove a chave, regravando a área de entradas
 *
 * Exemplo de uso:
 *   File idx = new File("animais.dat.idx");
 *   try (BPlusTreeIndex index = new BPlusTreeIndex(idx, (byte)1, 64)) {
 *       index.put(10, 1024L);
 *       Long off = index.get(10); // 1024
 *       index.remove(10);
 *   }
 */
public class BPlusTreeIndex<K extends Comparable<K>, V> implements AutoCloseable {

    private final RandomAccessFile raf;
    private FileHeaderHelper.BPlusTreeHeader header;
    private volatile boolean closed = false;

    // A implementação real da Árvore B+ em disco iria aqui.
    // Por enquanto, usamos um TreeMap para simular o comportamento em memória.
    private final TreeMap<K, V> map = new TreeMap<>();

    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

    public BPlusTreeIndex(File file, byte versaoFormato, int ordemDaArvore) throws IOException {
        this(file, versaoFormato, ordemDaArvore, (Serializer<K>) new IntegerSerializer(), (Serializer<V>) new LongSerializer());
    }

    public BPlusTreeIndex(File file, byte versaoFormato, int ordemDaArvore, Serializer<K> keySerializer, Serializer<V> valueSerializer) throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
        this.header = FileHeaderHelper.initBPlusIfEmpty(raf, versaoFormato, ordemDaArvore);
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        loadAll();
    }

    /** Carrega todas as entradas (key->offset) do arquivo para a memória. */
    private void loadAll() throws IOException {
        map.clear();
        raf.seek(FileHeaderHelper.HEADER_SIZE);
        while (raf.getFilePointer() < raf.length()) {
            K key = keySerializer.fromRAF(raf);
            V value = valueSerializer.fromRAF(raf);
            map.put(key, value);
        }
        if (map.size() != header.countTotalDeRegistros) {
            header.countTotalDeRegistros = map.size();
            FileHeaderHelper.writeBPlus(raf, header);
        }
    }

    /** Regrava a área de entradas com o conteúdo atual do TreeMap (ordenado). */
    private void persistAll() throws IOException {
        raf.setLength(FileHeaderHelper.HEADER_SIZE);
        raf.seek(FileHeaderHelper.HEADER_SIZE);
        for (var entry : map.entrySet()) {
            keySerializer.toRAF(raf, entry.getKey());
            valueSerializer.toRAF(raf, entry.getValue());
        }
        header.countTotalDeRegistros = map.size();
        FileHeaderHelper.writeBPlus(raf, header);
    }

    public synchronized void put(K key, V value) throws IOException {
        map.put(key, value);
        persistAll();
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void remove(K key) throws IOException {
        if (map.remove(key) != null) {
            persistAll();
        }
    }

    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        try {
            persistAll();
        } finally {
            raf.close();
            closed = true;
        }
    }

    // --- Serializers ---
    public interface Serializer<T> {
        void toRAF(RandomAccessFile raf, T value) throws IOException;
        T fromRAF(RandomAccessFile raf) throws IOException;
    }

    public static class IntegerSerializer implements Serializer<Integer> {
        @Override
        public void toRAF(RandomAccessFile raf, Integer value) throws IOException {
            raf.writeInt(value);
        }
        @Override
        public Integer fromRAF(RandomAccessFile raf) throws IOException {
            return raf.readInt();
        }
    }

    public static class LongSerializer implements Serializer<Long> {
        @Override
        public void toRAF(RandomAccessFile raf, Long value) throws IOException {
            raf.writeLong(value);
        }
        @Override
        public Long fromRAF(RandomAccessFile raf) throws IOException {
            return raf.readLong();
        }
    }

    public static class ArvoreElementoSerializer implements Serializer<ArvoreElemento> {
        @Override
        public void toRAF(RandomAccessFile raf, ArvoreElemento value) throws IOException {
            raf.write(value.toByteArray());
        }

        @Override
        public ArvoreElemento fromRAF(RandomAccessFile raf) throws IOException {
            ArvoreElemento el = new ArvoreElemento();
            byte[] bytes = new byte[el.size()];
            raf.readFully(bytes);
            el.fromByteArray(bytes);
            return el;
        }
    }

}
