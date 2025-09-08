package br.com.mpet.storage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Simple binary file storage with a tombstone flag per record.
 * Record format: [tombstone(byte)] [length(int)] [payload(bytes)]
 */
public class BinaryFileStore {
    private RandomAccessFile raf;
    private final Path path;
    private final PayloadCodec codec;

    public BinaryFileStore(Path path) throws IOException { this(path, PayloadCodec.identity()); }
    public BinaryFileStore(Path path, PayloadCodec codec) throws IOException {
        Files.createDirectories(path.getParent());
        this.path = path;
        this.codec = codec;
        this.raf = new RandomAccessFile(path.toFile(), "rw");
    }

    public synchronized long append(byte[] payload) throws IOException {
        raf.seek(raf.length());
        long pos = raf.getFilePointer();
        raf.writeByte(0); // active
    byte[] comp = codec.encode(payload);
    raf.writeInt(comp.length);
    raf.write(comp);
        return pos;
    }

    public synchronized Optional<byte[]> readAt(long pos) throws IOException {
        if (pos < 0) return Optional.empty();
        raf.seek(pos);
        int tomb = raf.readByte();
        int len = raf.readInt();
    byte[] data = new byte[len];
    raf.readFully(data);
        if (tomb != 0) return Optional.empty();
    return Optional.of(codec.decode(data));
    }

    public synchronized long updateAt(long pos, byte[] newPayload) throws IOException {
        raf.seek(pos);
        int tomb = raf.readByte();
        int len = raf.readInt();
        if (tomb != 0) return -1;
        byte[] comp = codec.encode(newPayload);
        if (comp.length <= len) {
            raf.write(comp);
            if (comp.length < len) {
                // pad remainder with zeros
                raf.write(new byte[len - comp.length]);
            }
            return pos;
        } else {
            // mark old as deleted and append new at end
            raf.seek(pos);
            raf.writeByte(1);
            return append(newPayload);
        }
    }

    public synchronized boolean deleteAt(long pos) throws IOException {
        if (pos < 0) return false;
        raf.seek(pos);
        raf.writeByte(1);
        return true;
    }

    public void close() throws IOException { raf.close(); }

    public Path getPath() { return path; }

    /**
     * Iterate all records, returning their file positions and payload if active.
     */
    public synchronized void forEach(RecordConsumer consumer) throws IOException {
        raf.seek(0);
        long fileLen = raf.length();
        long pos = 0;
        while (pos < fileLen) {
            raf.seek(pos);
            int tomb = raf.readByte();
            int len = raf.readInt();
            byte[] data = new byte[len];
            raf.readFully(data);
            if (tomb == 0) consumer.accept(pos, codec.decode(data));
            pos = raf.getFilePointer();
        }
    }

    @FunctionalInterface
    public interface RecordConsumer { void accept(long pos, byte[] payload) throws IOException; }

    /**
     * Compact the file by removing deleted records. Returns a map oldPos->newPos for index updates.
     */
    public synchronized java.util.Map<Long, Long> vacuum() throws IOException {
        java.util.Map<Long, Long> posMap = new java.util.HashMap<>();
        File tmp = File.createTempFile("vacuum", ".dat", path.getParent().toFile());
        try (RandomAccessFile out = new RandomAccessFile(tmp, "rw")) {
            raf.seek(0);
            long pos = 0;
            while (pos < raf.length()) {
                raf.seek(pos);
                int tomb = raf.readByte();
                int len = raf.readInt();
                byte[] data = new byte[len];
                raf.readFully(data);
                long next = raf.getFilePointer();
                if (tomb == 0) {
                    long newPos = out.getFilePointer();
                    out.writeByte(0);
                    out.writeInt(len);
                    out.write(data);
                    posMap.put(pos, newPos);
                }
                pos = next;
            }
        }
        raf.close();
        java.nio.file.Files.move(tmp.toPath(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        this.raf = new RandomAccessFile(path.toFile(), "rw");
        return posMap;
    }
}
