package br.com.mpet.persistence.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um "Bucket" (ou cesto) na estrutura de Hash Extensível.
 * Cada bucket armazena um conjunto de registros e tem uma profundidade local.
 *
 * @param <T> O tipo de registro, que deve estender RegistroHash.
 */
public class Bucket<T extends RegistroHash> {

    private final Constructor<T> constructor;
    private final int bucketSize; // Capacidade máxima de registros
    private final short recordSize;

    private int localDepth;
    private int count;
    private final List<T> records;

    /**
     * Construtor para um novo Bucket.
     *
     * @param constructor Construtor da classe de registro.
     * @param bucketSize  Capacidade do bucket.
     * @param localDepth  Profundidade local inicial.
     */
    public Bucket(Constructor<T> constructor, int bucketSize, int localDepth) throws IOException {
        this.constructor = constructor;
        this.bucketSize = bucketSize;
        this.localDepth = localDepth;
        this.count = 0;
        this.records = new ArrayList<>(bucketSize);
        try {
            this.recordSize = constructor.newInstance().size();
        } catch (Exception e) {
            throw new IOException("Não foi possível determinar o tamanho do registro.", e);
        }
    }

    public boolean isFull() {
        return count == bucketSize;
    }

    public boolean add(T record) {
        if (isFull()) {
            return false;
        }
        records.add(record);
        count++;
        return true;
    }

    public T find(int id) {
        for (T record : records) {
            if (record.getId() == id) {
                return record;
            }
        }
        return null;
    }

    public int getLocalDepth() {
        return localDepth;
    }

    public void setLocalDepth(int localDepth) {
        this.localDepth = localDepth;
    }

    public List<T> getRecords() {
        return records;
    }

    public int getCount() {
        return count;
    }

    /**
     * Serializa o bucket para um array de bytes de tamanho fixo.
     * Layout: [profundidadeLocal (int)] [count (int)] [registro1] [registro2] ... [padding]
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(getTotalSize());
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(localDepth);
        dos.writeInt(count);

        for (T record : records) {
            dos.write(record.toByteArray());
        }

        // Preenche com bytes vazios até atingir o tamanho total do bucket
        int paddingSize = (bucketSize - count) * recordSize;
        dos.write(new byte[paddingSize]);

        return baos.toByteArray();
    }

    /**
     * Deserializa um array de bytes para preencher o objeto Bucket.
     */
    public void fromByteArray(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        this.localDepth = dis.readInt();
        this.count = dis.readInt();
        this.records.clear();

        for (int i = 0; i < this.count; i++) {
            try {
                T record = constructor.newInstance();
                byte[] recordData = new byte[recordSize];
                dis.readFully(recordData);
                record.fromByteArray(recordData);
                this.records.add(record);
            } catch (Exception e) {
                throw new IOException("Falha ao deserializar registro do bucket.", e);
            }
        }
    }

    /**
     * Calcula o tamanho total em bytes que o bucket ocupa no arquivo.
     */
    public int getTotalSize() {
        // profundidadeLocal(int) + count(int) + (capacidade * tamanho do registro)
        return 4 + 4 + (bucketSize * recordSize);
    }
}