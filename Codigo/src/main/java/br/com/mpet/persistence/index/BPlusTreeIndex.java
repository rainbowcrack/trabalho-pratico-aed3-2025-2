package br.com.mpet.persistence.index;

import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
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
public class BPlusTreeIndex implements AutoCloseable {

    private final RandomAccessFile raf;
    private FileHeaderHelper.BPlusTreeHeader header;
    private volatile boolean closed = false;

    private final TreeMap<Integer, Long> map = new TreeMap<>();

    public BPlusTreeIndex(File file, byte versaoFormato, int ordemDaArvore) throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
        this.header = FileHeaderHelper.initBPlusIfEmpty(raf, versaoFormato, ordemDaArvore);
        loadAll();
    }

    /** Carrega todas as entradas (key->offset) do arquivo para a memória. */
    private void loadAll() throws IOException {
        map.clear();
        long pos = FileHeaderHelper.HEADER_SIZE;
        long len = raf.length();
        while (pos + 12 <= len) { // 4 bytes int + 8 bytes long
            raf.seek(pos);
            int key = raf.readInt();
            long off = raf.readLong();
            map.put(key, off);
            pos += 12;
        }
        header.countTotalDeRegistros = map.size();
        header.ponteiroParaNoRaiz = FileHeaderHelper.HEADER_SIZE; // placeholder
        FileHeaderHelper.writeBPlus(raf, header);
    }

    /** Regrava a área de entradas com o conteúdo atual do TreeMap (ordenado). */
    private void persistAll() throws IOException {
        // Reescreve a partir do HEADER_SIZE
        raf.setLength(FileHeaderHelper.HEADER_SIZE);
        raf.seek(FileHeaderHelper.HEADER_SIZE);
        for (Map.Entry<Integer, Long> e : map.entrySet()) {
            raf.writeInt(e.getKey());
            raf.writeLong(e.getValue());
        }
        header.countTotalDeRegistros = map.size();
        header.ponteiroParaNoRaiz = FileHeaderHelper.HEADER_SIZE; // ainda sem nós
        FileHeaderHelper.writeBPlus(raf, header);
    }

    /**
     * Insere ou atualiza uma chave no índice.
     * Exemplo: index.put(42, 2048L)
     */
    public synchronized void put(int key, long offset) throws IOException {
        map.put(key, offset);
        persistAll();
    }

    /**
     * Obtém o offset associado à chave ou null se não existir.
     * Exemplo: Long off = index.get(42);
     */
    public synchronized Long get(int key) {
        return map.get(key);
    }

    /**
     * Remove a chave do índice, se existir.
     * Exemplo: index.remove(42)
     */
    public synchronized void remove(int key) throws IOException {
        if (map.remove(key) != null) {
            persistAll();
        }
    }

    /**
     * Retorna o número de chaves no índice (apenas informativo).
     */
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        persistAll();
        raf.close();
        closed = true;
    }
}
