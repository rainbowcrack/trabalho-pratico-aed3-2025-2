package br.com.mpet.persistence;

import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Classe base para DAOs que persistem em arquivo binário.
 * Gerencia o cabeçalho, o acesso ao arquivo e operações de baixo nível.
 *
 * @param <T> Tipo da entidade a ser persistida.
 */
public abstract class BaseDataFile<T> implements Closeable {

    protected final File file;
    protected final RandomAccessFile raf;
    protected final byte versaoFormato;
    protected FileHeaderHelper.Header header;

    protected BaseDataFile(File file, byte versaoFormato) throws IOException {
        this.file = file;
        this.versaoFormato = versaoFormato;
        this.raf = new RandomAccessFile(file, "rw");
        if (raf.length() < FileHeaderHelper.HEADER_SIZE) {
            this.header = FileHeaderHelper.initIfEmpty(raf, versaoFormato);
            persistHeader();
        } else {
            this.header = FileHeaderHelper.read(raf);
            if (header.versaoFormato != versaoFormato) {
                throw new IOException("Versão do formato de arquivo incompatível.");
            }
        }
    }

    protected void persistHeader() throws IOException {
        FileHeaderHelper.write(raf, header);
    }

    protected int nextIdAndIncrement() throws IOException {
        int id = header.proximoId;
        header.proximoId++;
        persistHeader();
        return id;
    }

    protected void incrementCountAtivos() throws IOException {
        header.countAtivos++;
        persistHeader();
    }

    protected void decrementCountAtivos() throws IOException {
        header.countAtivos--;
        persistHeader();
    }

    protected long appendRecord(byte[] record) throws IOException {
        long offset = raf.length();
        raf.seek(offset);
        raf.write(record);
        return offset;
    }

    protected void overwritePayload(long offset, byte[] payload) throws IOException {
        raf.seek(offset);
        raf.write(payload);
    }

    protected void markTombstone(long offset) throws IOException {
        raf.seek(offset);
        raf.writeByte(1); // 1 = tombstone
    }

    protected byte[] readBytes(long offset, int len) throws IOException {
        byte[] buf = new byte[len];
        raf.seek(offset);
        raf.readFully(buf);
        return buf;
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            try {
                persistHeader();
            } finally {
                raf.close();
            }
        }
    }
}