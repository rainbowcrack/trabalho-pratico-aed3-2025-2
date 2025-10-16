package br.com.mpet.persistence.index;

public class Par<T, L> {
    public T chave;
    public L ponteiro;

    public Par(T chave, L ponteiro) {
        this.chave = chave;
        this.ponteiro = ponteiro;
    }
}