package br.com.mpet.persistence.index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;

public class BTree<T extends RegistroArvoreBMais<T>> implements AutoCloseable {

    private RandomAccessFile raf;
    private int ordem;
    private Constructor<T> construtor;
    private long raiz;
    private String nomeArquivo;

    public BTree(Constructor<T> construtor, int ordem, String nomeArquivo) throws IOException {
        this.construtor = construtor;
        this.ordem = ordem;
        this.nomeArquivo = nomeArquivo;
        this.raf = new RandomAccessFile(this.nomeArquivo, "rw");
        if (raf.length() == 0) {
            raf.writeLong(-1);
            raf.seek(0); // <<-- CORREÇÃO: Reposiciona o cursor para o início do arquivo
        }
        this.raiz = raf.readLong();
    }

    public long getRaiz() {
        return this.raiz;
    }

    public T read(int id) throws Exception {
        if (id < 0) {
            return null;
        }
        if (this.raiz != -1) {
            return this.read(this.raiz, id);
        }
        return null;
    }

    private T read(long pagina, int id) throws Exception {
        if (pagina != -1) {
            Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
            this.readPage(pagina, p);
            int i = 0;
            // A comparação deve ser feita com o ID real do elemento
            while (i < p.n && id > ((ArvoreElemento)p.chaves[i]).getId()) {
                i++;
            }
            if (i < p.n && id == ((ArvoreElemento)p.chaves[i]).getId()) {
                return p.chaves[i];
            }
            if (p.filhos[0] == -1) {
                return null;
            }
            return this.read(p.filhos[i], id);
        }
        return null;
    }

    public void update(int id, long newAddress) throws Exception {
        if (this.raiz != -1) {
            T aux = construtor.newInstance();
            this.update(this.raiz, id, newAddress);
        }
    }

    private void update(long pagina, int id, long newAddress) throws Exception {
        if (pagina != -1) {
            Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
            this.readPage(pagina, p);
            int i = 0;
            while (i < p.n && id > ((ArvoreElemento)p.chaves[i]).getId()) {
                i++;
            }
            if (i < p.n && id == ((ArvoreElemento)p.chaves[i]).getId()) {
                p.chaves[i] = (T) new ArvoreElemento(id, newAddress);
                this.writePage(pagina, p);
                return;
            }
            if (p.filhos[0] != -1) {
                this.update(p.filhos[i], id, newAddress);
            }
        }
    }

    public void delete(int id) throws Exception {
        if (this.raiz != -1) {
            T aux = construtor.newInstance();
            this.delete(this.raiz, id);
        }
    }

    private void delete(long pagina, int id) throws Exception {
        if (pagina != -1) {
            Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
            this.readPage(pagina, p);
            int i = 0;
            while (i < p.n && id > ((ArvoreElemento)p.chaves[i]).getId()) {
                i++;
            }
            if (i < p.n && id == ((ArvoreElemento)p.chaves[i]).getId()) {
                if (p.filhos[0] == -1) {
                    for (int j = i; j < p.n - 1; j++) {
                        p.chaves[j] = p.chaves[j + 1];
                    }
                    p.n--;
                    this.writePage(pagina, p);
                } else {
                    // Implementar a exclusão em nós internos (mais complexo)
                }
                return;
            }
            if (p.filhos[0] != -1) {
                this.delete(p.filhos[i], id);
            }
        }
    }

    public void create(T newChave) throws Exception {
        // Se a árvore estiver vazia, cria a primeira página raiz
        if (this.raiz == -1) {
            Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
            p.n = 1;
            p.chaves[0] = newChave;
            this.raiz = this.createPage(p);
            this.raf.seek(0);
            this.raf.writeLong(this.raiz);
            return;
        }

        // Inicia a inserção recursiva a partir da raiz
        Par<T, Long> promovido = create(this.raiz, newChave);

        // Se a recursão retornou uma chave promovida, a raiz foi dividida.
        // Cria uma nova raiz.
        if (promovido != null) {
            Pagina<T> newRoot = new Pagina<>(this.construtor, this.ordem);
            newRoot.n = 1;
            newRoot.chaves[0] = promovido.chave;
            newRoot.filhos[0] = this.raiz;
            newRoot.filhos[1] = promovido.ponteiro;
            this.raiz = this.createPage(newRoot);
            this.raf.seek(0);
            this.raf.writeLong(this.raiz);
        }
    }

    private Par<T, Long> create(long pagina, T newChave) throws Exception {
        Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
        readPage(pagina, p);

        // Encontra a posição correta para a chave na página atual
        int i = p.find(newChave);

        // Se a chave já existe, não faz nada (ou poderia atualizar)
        if (i < p.n && p.chaves[i].compareTo(newChave) == 0) {
            // Opcional: Lançar exceção ou atualizar o registro existente
            // System.err.println("Chave duplicada: " + newChave);
            return null;
        }

        // Se for um nó interno, desce recursivamente
        if (p.filhos[0] != -1) {
            Par<T, Long> promovido = create(p.filhos[i], newChave);

            // Se a chamada recursiva não retornou nada para promover, encerra.
            if (promovido != null) {
                return null;
            }

            // Insere a chave promovida na página atual (p)
            return insereEmPaginaNaoCheia(p, pagina, promovido.chave, promovido.ponteiro);
        }

        // Se for uma folha, insere diretamente
        return insereEmPaginaNaoCheia(p, pagina, newChave, -1L);
    }

    private Par<T, Long> insereEmPaginaNaoCheia(Pagina<T> p, long paginaEndereco, T chave, long filhoDireita) throws Exception {
        int i = p.find(chave);

        // Se a página tem espaço
        if (p.n < this.ordem) {
            for (int j = p.n; j > i; j--) {
                p.chaves[j] = p.chaves[j - 1];
                p.filhos[j + 1] = p.filhos[j];
            }
            p.chaves[i] = chave;
            p.filhos[i + 1] = filhoDireita;
            p.n++;
            writePage(paginaEndereco, p);
            return null; // Nada a promover
        }

        // Se a página está cheia, faz o SPLIT
        Pagina<T> newP = new Pagina<>(this.construtor, this.ordem);
        T[] tempChaves = (T[]) new RegistroArvoreBMais[this.ordem + 1];
        long[] tempFilhos = new long[this.ordem + 2];

        // Copia chaves e filhos para arrays temporários
        for (int j = 0; j < i; j++) {
            tempChaves[j] = p.chaves[j];
            tempFilhos[j] = p.filhos[j];
        }
        tempFilhos[i] = p.filhos[i];

        for (int j = i; j < this.ordem; j++) {
            tempChaves[j + 1] = p.chaves[j];
            tempFilhos[j + 1] = p.filhos[j];
        }
        tempFilhos[this.ordem + 1] = p.filhos[this.ordem];

        // Insere a nova chave e filho no temporário
        tempChaves[i] = chave;
        tempFilhos[i + 1] = filhoDireita;

        // Define o ponto de divisão (meio)
        int meio = (this.ordem + 1) / 2;
        T chavePromovida = tempChaves[meio];

        // Atualiza a página original (p) com a primeira metade
        p.n = meio;
        for (int j = 0; j < p.n; j++) {
            p.chaves[j] = tempChaves[j];
            p.filhos[j] = tempFilhos[j];
        }
        p.filhos[p.n] = tempFilhos[p.n];

        // Cria a nova página (newP) com a segunda metade
        newP.n = this.ordem - meio;
        for (int j = 0; j < newP.n; j++) {
            newP.chaves[j] = tempChaves[meio + 1 + j];
            newP.filhos[j] = tempFilhos[meio + 1 + j];
        }
        newP.filhos[newP.n] = tempFilhos[this.ordem + 1];

        // Se a página original era uma folha, a nova também é
        if (p.filhos[0] == -1) {
            newP.filhos[0] = -1;
        }

        // Encadeia as folhas
        newP.proximo = p.proximo;
        long newAddress = createPage(newP);
        p.proximo = newAddress;

        writePage(paginaEndereco, p);

        // Retorna a primeira chave da nova página para ser promovida ao pai
        return new Par<>(chavePromovida, newAddress);
    }

    private long createPage(Pagina<T> p) throws IOException {
        long endereco = this.raf.length();
        p.endereco = endereco;
        this.raf.seek(endereco);
        this.raf.write(p.toByteArray());
        return endereco;
    }

    private void readPage(long endereco, Pagina<T> p) throws IOException {
        this.raf.seek(endereco);
        byte[] ba = new byte[p.size()];
        this.raf.read(ba);
        p.fromByteArray(ba);
        p.endereco = endereco;
    }

    private void writePage(long endereco, Pagina<T> p) throws IOException {
        this.raf.seek(endereco);
        this.raf.write(p.toByteArray());
    }

    public void print() throws Exception {
        if (this.raiz != -1) {
            this.print(this.raiz, 0);
        }
    }

    private void print(long pagina, int level) throws Exception {
        if (pagina != -1) {
            Pagina<T> p = new Pagina<>(this.construtor, this.ordem);
            this.readPage(pagina, p);
            System.out.print("Level " + level + ": ");
            p.print();

            if (p.filhos[0] != -1) {
                for (int i = 0; i <= p.n; i++) {
                    print(p.filhos[i], level + 1);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }
}