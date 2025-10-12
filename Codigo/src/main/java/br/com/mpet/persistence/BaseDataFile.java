package br.com.mpet.persistence;

import br.com.mpet.persistence.io.FileHeaderHelper;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Base utilitária para manipulação de arquivo .dat.
 */
public abstract class BaseDataFile implements Closeable {

    protected final File file;
    protected final RandomAccessFile raf;
    protected FileHeaderHelper.Header header;
    protected final byte versaoFormato;
    protected volatile boolean closed = false;

    protected BaseDataFile(File file, byte versaoFormato) throws IOException {
        this.file = file;
        this.versaoFormato = versaoFormato;
        this.raf = new RandomAccessFile(file, "rw");
        this.header = FileHeaderHelper.initIfEmpty(raf, versaoFormato);
    }

    protected synchronized int nextIdAndIncrement() throws IOException {
        int current = header.proximoId;
        header.proximoId++;
        persistHeader();
        return current;
    }

    protected synchronized void incrementCountAtivos() throws IOException {
        header.countAtivos++;
        persistHeader();
    }

    protected synchronized void decrementCountAtivos() throws IOException {
        header.countAtivos = Math.max(0, header.countAtivos - 1);
        persistHeader();
    }

    protected synchronized void persistHeader() throws IOException {
        FileHeaderHelper.write(raf, header);
    }

    protected synchronized long appendRecord(byte[] fullRecord) throws IOException {
        long offset = raf.length();
        raf.seek(offset);
        raf.write(fullRecord);
        return offset;
    }

    protected synchronized void markTombstone(long offset) throws IOException {
        raf.seek(offset + 1); // pula tipoRegistro
        raf.writeByte(1);
    }

    protected synchronized byte[] readBytes(long offset, int totalLength) throws IOException {
        byte[] buf = new byte[totalLength];
        raf.seek(offset);
        raf.readFully(buf);
        return buf;
    }

    protected synchronized void overwritePayload(long payloadOffset, byte[] newPayload) throws IOException {
        raf.seek(payloadOffset);
        raf.write(newPayload);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        raf.close();
    }
}
