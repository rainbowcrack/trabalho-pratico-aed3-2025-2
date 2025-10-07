package br.com.mpet.persistence.io;

import java.io.IOException;
import java.io.RandomAccessFile;
    
/**
 * Gerencia cabeçalho fixo de 128 bytes no início do arquivo .dat.
 * Layout:
 * [0]     byte versaoFormato
 * [1-4]   int  proximoId
 * [5-8]   int  countAtivos
 * [9-127] reservado
 */
public final class FileHeaderHelper {
    public static final int HEADER_SIZE = 128;

    private FileHeaderHelper() {}

    public static class Header {
        public byte versaoFormato;
        public int proximoId;
        public int countAtivos;
    }

    /**
     * Cabeçalho específico para arquivo de índice de Hash Extensível.
     * Tamanho sugerido também 128 bytes (somente 1 uso por enquanto, restante padding).
     * Campos principais:
     *  - versaoFormato: controle de evolução de layout
     *  - profundidadeGlobal: d (número de bits usados do hash)
     *  - tamanhoDoBucket: capacidade (nº de pares chave/ponteiro) por bucket
     *  - ponteiroParaDiretorio: offset onde começa o diretório (array de 2^d ponteiros)
     *  - countTotalDeRegistros: quantidade de entradas ativas (para estatísticas / fator de carga)
     */
    public static class HashFileHeader {
        public byte versaoFormato;
        public int profundidadeGlobal;      // principal para expansão
        public int tamanhoDoBucket;         // slots por bucket
        public long ponteiroParaDiretorio;  // offset do diretório
        public int countTotalDeRegistros;   // total de pares chave->offset
    }

    /**
     * Cabeçalho específico para arquivo de índice Árvore B+.
     * Campos:
     *  - versaoFormato: evolução de layout
     *  - ponteiroParaNoRaiz: offset do nó raiz atual
     *  - ordemDaArvore: ordem (máximo de filhos) usada para calcular splits
     *  - alturaDaArvore: cache para evitar percorrer para medir profundidade
     *  - countTotalDeRegistros: total de chaves armazenadas (folhas)
     *  - ponteiroParaListaDeNosLivres: encadeamento de nós reutilizáveis
     */
    public static class BPlusTreeHeader {
        public byte versaoFormato;
        public long ponteiroParaNoRaiz;
        public int ordemDaArvore;
        public int alturaDaArvore;
        public int countTotalDeRegistros;
        public long ponteiroParaListaDeNosLivres;
    }

    /* =============================================================
     * HEADER PRINCIPAL (já existente)
     * ============================================================= */

    public static Header read(RandomAccessFile raf) throws IOException {
        if (raf.length() < HEADER_SIZE) throw new IOException("Cabeçalho ausente ou corrompido");
        raf.seek(0);
        Header h = new Header();
        h.versaoFormato = raf.readByte();
        h.proximoId = raf.readInt();
        h.countAtivos = raf.readInt();
        return h;
    }

    public static void write(RandomAccessFile raf, Header h) throws IOException {
        raf.seek(0);
        raf.writeByte(h.versaoFormato);
        raf.writeInt(h.proximoId);
        raf.writeInt(h.countAtivos);
        long written = 1 + 4 + 4;
        long remaining = HEADER_SIZE - written;
        for (long i = 0; i < remaining; i++) raf.writeByte(0);
    }

    public static Header initIfEmpty(RandomAccessFile raf, byte versao) throws IOException {
        if (raf.length() == 0) {
            Header h = new Header();
            h.versaoFormato = versao;
            h.proximoId = 1;
            h.countAtivos = 0;
            write(raf, h);
            return h;
        }
        return read(raf);
    }

    /* =============================================================
     * HASH EXTENSÍVEL - LAYOUT (128 bytes):
     * 0      : byte versaoFormato
     * 1-4    : int profundidadeGlobal
     * 5-8    : int tamanhoDoBucket
     * 9-16   : long ponteiroParaDiretorio
     * 17-20  : int countTotalDeRegistros
     * 21-127 : padding (zeros)
     * ============================================================= */
    public static HashFileHeader readHash(RandomAccessFile raf) throws IOException {
        if (raf.length() < HEADER_SIZE) throw new IOException("Cabeçalho hash ausente ou corrompido");
        raf.seek(0);
        HashFileHeader h = new HashFileHeader();
        h.versaoFormato = raf.readByte();
        h.profundidadeGlobal = raf.readInt();
        h.tamanhoDoBucket = raf.readInt();
        h.ponteiroParaDiretorio = raf.readLong();
        h.countTotalDeRegistros = raf.readInt();
        return h;
    }

    public static void writeHash(RandomAccessFile raf, HashFileHeader h) throws IOException {
        raf.seek(0);
        raf.writeByte(h.versaoFormato);
        raf.writeInt(h.profundidadeGlobal);
        raf.writeInt(h.tamanhoDoBucket);
        raf.writeLong(h.ponteiroParaDiretorio);
        raf.writeInt(h.countTotalDeRegistros);
        long written = 1 + 4 + 4 + 8 + 4; // 21
        for (long i = written; i < HEADER_SIZE; i++) raf.writeByte(0);
    }

    /**
     * Inicializa arquivo de índice hash se vazio. profundidadeGlobal inicial tipicamente 1.
     * ponteiroParaDiretorio pode começar após o cabeçalho (HEADER_SIZE) ou 0 (adiado) – aqui adotamos HEADER_SIZE.
     */
    public static HashFileHeader initHashIfEmpty(RandomAccessFile raf, byte versao, int tamanhoBucket) throws IOException {
        if (raf.length() == 0) {
            HashFileHeader h = new HashFileHeader();
            h.versaoFormato = versao;
            h.profundidadeGlobal = 1;
            h.tamanhoDoBucket = tamanhoBucket;
            h.ponteiroParaDiretorio = HEADER_SIZE; // diretório começará logo após header
            h.countTotalDeRegistros = 0;
            writeHash(raf, h);
            return h;
        }
        return readHash(raf);
    }

    /* =============================================================
     * ÁRVORE B+ - LAYOUT (128 bytes):
     * 0      : byte versaoFormato
     * 1-8    : long ponteiroParaNoRaiz
     * 9-12   : int ordemDaArvore
     * 13-16  : int alturaDaArvore
     * 17-20  : int countTotalDeRegistros
     * 21-28  : long ponteiroParaListaDeNosLivres
     * 29-127 : padding (zeros)
     * ============================================================= */
    public static BPlusTreeHeader readBPlus(RandomAccessFile raf) throws IOException {
        if (raf.length() < HEADER_SIZE) throw new IOException("Cabeçalho B+ ausente ou corrompido");
        raf.seek(0);
        BPlusTreeHeader h = new BPlusTreeHeader();
        h.versaoFormato = raf.readByte();
        h.ponteiroParaNoRaiz = raf.readLong();
        h.ordemDaArvore = raf.readInt();
        h.alturaDaArvore = raf.readInt();
        h.countTotalDeRegistros = raf.readInt();
        h.ponteiroParaListaDeNosLivres = raf.readLong();
        return h;
    }

    public static void writeBPlus(RandomAccessFile raf, BPlusTreeHeader h) throws IOException {
        raf.seek(0);
        raf.writeByte(h.versaoFormato);
        raf.writeLong(h.ponteiroParaNoRaiz);
        raf.writeInt(h.ordemDaArvore);
        raf.writeInt(h.alturaDaArvore);
        raf.writeInt(h.countTotalDeRegistros);
        raf.writeLong(h.ponteiroParaListaDeNosLivres);
        long written = 1 + 8 + 4 + 4 + 4 + 8; // 29
        for (long i = written; i < HEADER_SIZE; i++) raf.writeByte(0);
    }

    /**
     * Inicializa arquivo de índice B+ se vazio. Raiz começa inexistente (-1) até primeiro split/folha criada.
     */
    public static BPlusTreeHeader initBPlusIfEmpty(RandomAccessFile raf, byte versao, int ordem) throws IOException {
        if (raf.length() == 0) {
            BPlusTreeHeader h = new BPlusTreeHeader();
            h.versaoFormato = versao;
            h.ponteiroParaNoRaiz = -1L; // sem nó raiz ainda
            h.ordemDaArvore = ordem;
            h.alturaDaArvore = 0; // 0 enquanto não criamos a primeira folha
            h.countTotalDeRegistros = 0;
            h.ponteiroParaListaDeNosLivres = 0L; // nenhum nó livre
            writeBPlus(raf, h);
            return h;
        }
        return readBPlus(raf);
    }
}
