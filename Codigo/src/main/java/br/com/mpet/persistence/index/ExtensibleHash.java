package br.com.mpet.persistence.index;

import br.com.mpet.persistence.io.FileHeaderHelper;
import br.com.mpet.persistence.io.FileHeaderHelper.HashFileHeader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;

/**
 * Implementação de Hash Extensível para armazenamento em arquivo.
 * Gerencia um diretório de ponteiros e buckets de dados em um único arquivo.
 *
 * @param <T> O tipo de registro, que deve estender RegistroHash.
 */
public class ExtensibleHash<T extends RegistroHash> implements AutoCloseable {

    private final RandomAccessFile raf;
    private final Constructor<T> constructor;
    private HashFileHeader header;
    private long[] directory;

    private static final byte FILE_VERSION = 1;

    public ExtensibleHash(Constructor<T> constructor, String filePath, int bucketSize) throws IOException {
        this.constructor = constructor;
        this.raf = new RandomAccessFile(filePath, "rw");
        this.header = FileHeaderHelper.initHashIfEmpty(raf, FILE_VERSION, bucketSize);

        if (raf.length() == FileHeaderHelper.HEADER_SIZE) {
            // primeiro bucket
            initializeFirstBucket();
        }

        loadDirectory();
    }

    private void initializeFirstBucket() throws IOException {
        // profundidade global inicial em 1
        this.directory = new long[2];

        // primeiro bucket
        Bucket<T> firstBucket = new Bucket<>(constructor, header.tamanhoDoBucket, 1);
        long bucketAddress = raf.length(); 

        directory[0] = bucketAddress;
        directory[1] = bucketAddress;

        // escreve o diretorio e o bucket no arquivo
        writeDirectory();
        writeBucket(bucketAddress, firstBucket);
    }

    private int getDirectoryIndex(int id) {
        int hash = Integer.hashCode(id);
        int mask = (1 << header.profundidadeGlobal) - 1;
        return hash & mask;
    }

    public boolean create(T newRecord) throws IOException {
        int index = getDirectoryIndex(newRecord.getId());
        long bucketAddress = directory[index];
        Bucket<T> bucket = readBucket(bucketAddress);

        if (bucket.find(newRecord.getId()) != null) {
            return false;
        }

        if (!bucket.isFull()) {
            bucket.add(newRecord);
            writeBucket(bucketAddress, bucket);
            header.countTotalDeRegistros++;
            FileHeaderHelper.writeHash(raf, header);
            return true;
        }

        if (bucket.getLocalDepth() == header.profundidadeGlobal) {
            duplicateDirectory();
        }

        // um novo bucket e redistribui os registros
        int newLocalDepth = bucket.getLocalDepth() + 1;
        bucket.setLocalDepth(newLocalDepth);

        Bucket<T> newBucket = new Bucket<>(constructor, header.tamanhoDoBucket, newLocalDepth);
        long newBucketAddress = raf.length();

        // redistribui ponteiros
        int oldIndexMask = (1 << (newLocalDepth - 1)) - 1;
        int oldBucketBaseIndex = getDirectoryIndex(newRecord.getId()) & oldIndexMask;
        int newPointerIndexStart = oldBucketBaseIndex | (1 << (newLocalDepth - 1));

        for (int i = newPointerIndexStart; i < directory.length; i += (1 << newLocalDepth)) {
            directory[i] = newBucketAddress;
        }

        // escreve novo bucket
        writeDirectory();
        writeBucket(newBucketAddress, newBucket);

        bucket.getRecords().add(newRecord);
        Bucket<T> tempOldBucket = new Bucket<>(constructor, header.tamanhoDoBucket, newLocalDepth);

        for (T record : bucket.getRecords()) {
            int idx = getDirectoryIndex(record.getId());
            if (directory[idx] == bucketAddress) {
                tempOldBucket.add(record);
            } else {
                newBucket.add(record);
            }
        }

        // buckets atualizados
        writeBucket(bucketAddress, tempOldBucket);
        writeBucket(newBucketAddress, newBucket);

        header.countTotalDeRegistros++;
        FileHeaderHelper.writeHash(raf, header);

        return true;
    }

    public T read(int id) throws IOException {
        int index = getDirectoryIndex(id);
        long bucketAddress = directory[index];
        Bucket<T> bucket = readBucket(bucketAddress);
        return bucket.find(id);
    }

    public boolean update(T updatedRecord) throws IOException {
        int index = getDirectoryIndex(updatedRecord.getId());
        long bucketAddress = directory[index];
        Bucket<T> bucket = readBucket(bucketAddress);

        for (int i = 0; i < bucket.getCount(); i++) {
            if (bucket.getRecords().get(i).getId() == updatedRecord.getId()) {
                bucket.getRecords().set(i, updatedRecord);
                writeBucket(bucketAddress, bucket);
                return true;
            }
        }
        return false; 
    }

    public boolean delete(int id) throws IOException {
        int index = getDirectoryIndex(id);
        long bucketAddress = directory[index];
        Bucket<T> bucket = readBucket(bucketAddress);

        boolean removed = bucket.getRecords().removeIf(record -> record.getId() == id);

        if (removed) {
            writeBucket(bucketAddress, bucket);
            header.countTotalDeRegistros--;
            FileHeaderHelper.writeHash(raf, header);
        }


        return removed;
    }

    private void duplicateDirectory() throws IOException {
        header.profundidadeGlobal++;
        int oldSize = directory.length;
        int newSize = oldSize * 2;
        long[] newDirectory = new long[newSize];

        // copia os ponteiros antigos para o novo diretorio duplicado
        System.arraycopy(directory, 0, newDirectory, 0, oldSize);
        System.arraycopy(directory, 0, newDirectory, oldSize, oldSize);

        this.directory = newDirectory;
        FileHeaderHelper.writeHash(raf, header);
        writeDirectory();
    }

    private void loadDirectory() throws IOException {
        int dirSize = 1 << header.profundidadeGlobal;
        this.directory = new long[dirSize];
        raf.seek(header.ponteiroParaDiretorio);
        for (int i = 0; i < dirSize; i++) {
            this.directory[i] = raf.readLong();
        }
    }

    private void writeDirectory() throws IOException {
        raf.seek(header.ponteiroParaDiretorio);
        for (long address : directory) {
            raf.writeLong(address);
        }
    }

    private Bucket<T> readBucket(long address) throws IOException {
        Bucket<T> bucket = new Bucket<>(constructor, header.tamanhoDoBucket, 0);
        byte[] data = new byte[bucket.getTotalSize()];
        raf.seek(address);
        raf.readFully(data);
        bucket.fromByteArray(data);
        return bucket;
    }

    private void writeBucket(long address, Bucket<T> bucket) throws IOException {
        raf.seek(address);
        raf.write(bucket.toByteArray());
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }

    public void print() throws IOException {
        System.out.println("--- CABEÇALHO ---");
        System.out.println("Versão: " + header.versaoFormato);
        System.out.println("Profundidade Global: " + header.profundidadeGlobal);
        System.out.println("Tamanho do Bucket: " + header.tamanhoDoBucket);
        System.out.println("Total de Registros: " + header.countTotalDeRegistros);
        System.out.println("\n--- DIRETÓRIO ---");
        for (int i = 0; i < directory.length; i++) {
            System.out.println("Índice " + i + " -> Endereço: " + directory[i]);
        }
        System.out.println("\n--- BUCKETS ---");
       
    }
}