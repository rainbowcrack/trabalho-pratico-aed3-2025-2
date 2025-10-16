package br.com.mpet.persistence.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

public class Pagina<T extends RegistroArvoreBMais<T>> {

    protected int ordem;
    protected int n;
    protected T[] chaves;
    protected long[] filhos;
    protected long endereco;
    protected long proximo;
    protected final Constructor<T> construtor;

    public Pagina(Constructor<T> construtor, int ordem) {
        this.construtor = construtor;
        this.ordem = ordem;
        this.n = 0;
        this.chaves = (T[]) new RegistroArvoreBMais[ordem];
        this.filhos = new long[ordem + 1];
        for (int i = 0; i < ordem + 1; i++) {
            this.filhos[i] = -1;
        }
        this.endereco = -1;
        this.proximo = -1;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(n);
        dos.writeLong(proximo);

        int i = 0;
        while (i < n) {
            dos.writeLong(filhos[i]);
            dos.write(chaves[i].toByteArray());
            i++;
        }
        dos.writeLong(filhos[i]);

        // Preenche o restante da página com bytes nulos para manter o tamanho fixo
        short tamanhoRegistro;
        try {
            tamanhoRegistro = construtor.newInstance().size();
        } catch (Exception e) {
            tamanhoRegistro = 12; // Fallback para ArvoreElemento
        }

        for (int j = i; j < ordem; j++) {
            dos.write(new byte[tamanhoRegistro]); // Chave vazia
            dos.writeLong(-1L);                   // Ponteiro filho nulo
        }
        return baos.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.n = dis.readInt();
        this.proximo = dis.readLong();
        
        short tamanhoRegistro;
        try {
            tamanhoRegistro = construtor.newInstance().size();
        } catch (Exception e) {
            tamanhoRegistro = 12; // Fallback
        }
        
        int i = 0;
        for (i = 0; i < ordem; i++) {
            this.filhos[i] = dis.readLong();
            byte[] reg = new byte[tamanhoRegistro];
            dis.read(reg);
            if (i < n) {
                try {
                    this.chaves[i] = construtor.newInstance();
                    this.chaves[i].fromByteArray(reg);
                } catch (Exception e) {
                    throw new IOException("Falha ao instanciar registro da árvore B+", e);
                }
            }
        }
        this.filhos[i] = dis.readLong();
    }

    public int size() {
        try {
            short tamanhoRegistro = construtor.newInstance().size();
            // n(int) + proximo(long) + (ordem+1)*filhos(long) + ordem*chaves(tamanhoRegistro)
            return 4 + 8 + ((ordem + 1) * 8) + (ordem * tamanhoRegistro);
        } catch (Exception e) {
            // Fallback para ArvoreElemento
            return 4 + 8 + ((ordem + 1) * 8) + (ordem * 12);
        }
    }

    public void print() {
        System.out.println("Endereço: " + this.endereco);
        System.out.println("Número de elementos: " + this.n);
        System.out.println("Próximo: " + this.proximo);
        int i = 0;
        while (i < this.n) {
            System.out.println("Filho: " + this.filhos[i]);
            System.out.println("Chave: " + this.chaves[i]);
            i++;
        }
        System.out.println("Filho: " + this.filhos[i]);
    }

    public int find(T chave) {
        if (this.n == 0) {
            return 0;
        }

        int i = 0;
        while (i < this.n && chave.compareTo(this.chaves[i]) > 0) {
            i++;
        }
        return i;
    }
}