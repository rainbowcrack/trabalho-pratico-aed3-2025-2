package br.com.mpet.persistence.dao;

import br.com.mpet.model.Animal;
import br.com.mpet.model.Cachorro;
import br.com.mpet.model.Gato;
import br.com.mpet.model.Porte;
import br.com.mpet.model.NivelAdestramento;
import br.com.mpet.persistence.io.Codec;
import br.com.mpet.persistence.index.BPlusTreeIndex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação de DAO para a hierarquia de {@link Animal} (polimórfico),
 * persistindo os registros em arquivo binário .dat com o seguinte layout:
 *
 * Arquivo:
 *   - Cabeçalho fixo de 128 bytes (gerenciado por {@link br.com.mpet.persistence.io.FileHeaderHelper})
 *   - Sequência de registros: [tipo][tombstone][id][len][payload]
 *
 * Registro (offset = posição onde começa o registro):
 *   - +0: tipo       (1 byte)  -> 1=CACHORRO, 2=GATO
 *   - +1: tombstone  (1 byte)  -> 0=ativo, 1=removido
 *   - +2: id         (4 bytes) -> int (big-endian)
 *   - +6: len        (4 bytes) -> tamanho do payload em bytes
 *   - +10: payload   (len bytes)
 *
 * O payload é codificado por {@link Codec} e segue a ordem de campos por classe:
 *
 * Campos comuns de Animal (nesta ordem):
 *   idOng (int), nome (StringU16), dataNascimentoAprox (LocalDate),
 *   sexo (char), porte (Enum), vacinado (bool), descricao (StringU16)
 *
 * Cachorro (após os campos comuns):
 *   raca (StringU16), nivelAdestramento (Enum),
 *   seDaBemComCachorros (bool), seDaBemComGatos (bool), seDaBemComCriancas (bool)
 *
 * Gato (após os campos comuns):
 *   raca (StringU16), seDaBemComCachorros (bool), seDaBemComGatos (bool), seDaBemComCriancas (bool),
 *   acessoExterior (bool), possuiTelamento (bool)
 *
 * Notas:
 * - Booleans são codificados como 1 byte 'V' (true) ou 'F' (false) usando Codec.encodeTriBoolean/dec.
 * - Strings seguem convenção do Codec: null=0xFFFF, ""=0.
 * - Em update com mudança de tamanho do payload, fazemos tombstone do registro antigo e append de novo registro.
 */
public class AnimalDataFileDao extends AnimalDao {

    // Tipos de registro no byte "tipo"
    private static final byte TIPO_CACHORRO = 1;
    private static final byte TIPO_GATO = 2;

    // Constantes de layout
    private static final int REC_POS_TIPO = 0;           // +0
    private static final int REC_POS_TOMBSTONE = 1;      // +1
    private static final int REC_POS_ID = 2;             // +2..+5 (int)
    private static final int REC_POS_LEN = 6;            // +6..+9 (int)
    private static final int REC_POS_PAYLOAD = 10;       // +10..+10+len-1

    // Índice primário (id -> offset) usando B+ simplificado em arquivo .idx
    private final Map<Integer, Long> indexById = new HashMap<>(); // cache em memória
    private final BPlusTreeIndex bplus;

    public AnimalDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
        // Arquivo de índice B+ (mesmo nome + .idx)
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        this.bplus = new BPlusTreeIndex(idxFile, versaoFormato, 64);
        rebuildIfEmpty();
    }

    /* =============================================================
     * CRUD
     * ============================================================= */
    /**
     * Cria um novo registro de Animal (Cachorro ou Gato).
     * Exemplo de uso:
     *   var dao = new AnimalDataFileDao(new File("animais.dat"), (byte)1);
     *   var dog = new Cachorro();
     *   dog.setNome("Thor"); dog.setIdOng(1); dog.setPorte(Porte.MEDIO); dog.setSexo('M');
     *   dao.create(dog);
     */
    @Override
    public synchronized Animal create(Animal entity) throws IOException {
    if (entity == null) throw new IllegalArgumentException("entity == null");
    if (!entity.isAtivo()) entity.setAtivo(true);

        // Atribui ID sequencial
    entity.setId(nextIdAndIncrement());

        byte tipo = tipoPara(entity);
        byte[] payload = encodeAnimal(entity);
        byte[] full = montarRegistro(tipo, (byte)0, entity.getId(), payload);
        long offset = appendRecord(full);

        indexById.put(entity.getId(), offset);
        bplus.put(entity.getId(), offset);
        incrementCountAtivos();
        return entity;
    }

    /**
     * Lê um Animal ativo pelo ID.
     * Exemplo: Optional<Animal> a = dao.read(10);
     */
    @Override
    public synchronized Optional<Animal> read(Integer id) throws IOException {
        if (id == null) return Optional.empty();
        Long off = indexById.get(id);
        if (off == null) {
            // fallback para índice B+
            off = bplus.get(id);
            if (off != null) indexById.put(id, off);
        }
        if (off == null) return Optional.empty();
        Animal a = readAtOffset(off);
        return Optional.ofNullable(a);
    }

    /**
     * Atualiza um Animal existente.
     * Se o payload tiver tamanho diferente, faz tombstone e apenda um novo registro.
     * Exemplo:
     *   var a = dao.read(10).orElseThrow(); a.setNome("Novo Nome"); dao.update(a);
     */
    @Override
    public synchronized boolean update(Animal entity) throws IOException {
        if (entity == null) return false;
    Long off = indexById.get(entity.getId());
        if (off == null) return false;

        // Lê header do registro atual para obter length
        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb != 0) return false; // já removido
        raf.seek(off + REC_POS_LEN);
        int oldLen = raf.readInt();

        byte[] newPayload = encodeAnimal(entity);
        if (newPayload.length == oldLen) {
            // Atualização in-place do payload
            overwritePayload(off + REC_POS_PAYLOAD, newPayload);
            return true;
        } else {
            // Marca como removido e apenda um novo registro
            markTombstone(off);
            decrementCountAtivos();

            byte tipo = tipoPara(entity);
            byte[] full = montarRegistro(tipo, (byte)0, entity.getId(), newPayload);
            long newOff = appendRecord(full);
            indexById.put(entity.getId(), newOff);
            bplus.put(entity.getId(), newOff);
            incrementCountAtivos();
            return true;
        }
    }

    /**
     * Remove logicamente (tombstone) um Animal pelo ID.
     * Exemplo: dao.delete(10);
     */
    @Override
    public synchronized boolean delete(Integer id) throws IOException {
        if (id == null) return false;
        Long off = indexById.get(id);
        if (off == null) return false;
        // Se ainda ativo, marca tombstone e ajusta contadores
        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb == 0) {
            markTombstone(off);
            decrementCountAtivos();
        }
        indexById.remove(id);
        bplus.remove(id);
        return true;
    }

    /**
     * Lista todos os Animais ativos varrendo o arquivo. Custo O(n).
     * Exemplo: List<Animal> todos = dao.listAllActive();
     */
    @Override
    public synchronized List<Animal> listAllActive() throws IOException {
        List<Animal> list = new ArrayList<>();
        long len = raf.length();
        long pos = br.com.mpet.persistence.io.FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (tomb == 0) {
                Animal a = readAtOffset(pos);
                if (a != null) list.add(a);
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        return list;
    }

    /**
     * Reconstrói o índice in-memory varrendo o arquivo, e sincroniza o índice B+ no disco.
     * Chamado no construtor; pode ser invocado se o cache for perdido.
     */
    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexById.isEmpty()) return;
        indexById.clear();
        int ativos = 0;
        long len = raf.length();
        long pos = br.com.mpet.persistence.io.FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_ID);
            int id = raf.readInt();
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (payloadLen < 0) break; // corrupção
            if (tomb == 0) {
                indexById.put(id, pos);
                // Atualiza índice B+
                bplus.put(id, pos);
                ativos++;
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        if (ativos != header.countAtivos) {
            header.countAtivos = ativos;
            persistHeader();
        }
    }

    /**
     * Compacta o arquivo removendo registros tombstoned (gera arquivo temporário e substitui).
     * Exemplo: dao.vacuum();
     */
    @Override
    public synchronized void vacuum() throws IOException {
        // Cria arquivo temporário e regrava apenas registros ativos
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (AnimalDataFileDao novo = new AnimalDataFileDao(temp, this.versaoFormato)) {
            for (Animal a : listAllActive()) {
                novo.create(cloneForVacuum(a));
            }
        }
        // Substitui arquivo antigo pelo compactado (dados e índice)
        // Fechamos este DAO para liberar handles antes de substituir
        this.close();

        // 1) Substitui o .dat
        if (!file.delete()) throw new IOException("Falha ao apagar arquivo antigo: " + file);
        if (!temp.renameTo(file)) throw new IOException("Falha ao renomear arquivo temporário: " + temp);

        // 2) Substitui o .idx correspondente
        File mainIdx = new File(file.getParentFile(), file.getName() + ".idx");
        File tempIdx = new File(temp.getParentFile(), temp.getName() + ".idx");
        if (tempIdx.exists()) {
            // remove índice antigo, se houver
            if (mainIdx.exists() && !mainIdx.delete()) {
                throw new IOException("Falha ao apagar índice antigo: " + mainIdx);
            }
            if (!tempIdx.renameTo(mainIdx)) {
                throw new IOException("Falha ao renomear índice temporário: " + tempIdx);
            }
        }
    }

    /* =============================================================
     * Helpers de serialização
     * ============================================================= */
    private byte tipoPara(Animal a) {
        if (a instanceof Cachorro) return TIPO_CACHORRO;
        if (a instanceof Gato) return TIPO_GATO;
        throw new IllegalArgumentException("Tipo de animal não suportado: " + a.getClass());
    }

    private byte[] montarRegistro(byte tipo, byte tombstone, int id, byte[] payload) {
        byte[] idb = Codec.encodeInt(id);
        byte[] lenb = Codec.encodeInt(payload.length);
        byte[] header = new byte[]{ tipo, tombstone, idb[0], idb[1], idb[2], idb[3], lenb[0], lenb[1], lenb[2], lenb[3] };
        return Codec.concat(header, payload);
    }

    private Animal readAtOffset(long offset) throws IOException {
        raf.seek(offset + REC_POS_TIPO);
        int tipo = raf.readUnsignedByte();
        raf.seek(offset + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        raf.seek(offset + REC_POS_ID);
        int id = raf.readInt();
        raf.seek(offset + REC_POS_LEN);
        int payloadLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payloadLen);

        Animal a = switch (tipo) {
            case TIPO_CACHORRO -> decodeCachorro(id, tomb, buf);
            case TIPO_GATO -> decodeGato(id, tomb, buf);
            default -> null;
        };
        return a;
    }

    private byte[] encodeAnimal(Animal a) {
        // Comuns de Animal
    byte[] idOng = Codec.encodeInt(a.getIdOng());
    byte[] nome = Codec.encodeStringU16(a.getNome());
    byte[] nasc = Codec.encodeLocalDate(a.getDataNascimentoAprox());
    byte[] sexo = Codec.encodeChar(a.getSexo());
    byte[] porte = Codec.encodeEnum(a.getPorte());
    byte[] vac = Codec.encodeTriBoolean(a.isVacinado());
    byte[] desc = Codec.encodeStringU16(a.getDescricao());

        if (a instanceof Cachorro c) {
            byte[] raca = Codec.encodeStringU16(c.getRaca());
            byte[] nivel = Codec.encodeEnum(c.getNivelAdestramento());
            byte[] b1 = Codec.encodeTriBoolean(c.isSeDaBemComCachorros());
            byte[] b2 = Codec.encodeTriBoolean(c.isSeDaBemComGatos());
            byte[] b3 = Codec.encodeTriBoolean(c.isSeDaBemComCriancas());
            return Codec.concat(idOng, nome, nasc, sexo, porte, vac, desc, raca, nivel, b1, b2, b3);
        } else if (a instanceof Gato g) {
            byte[] raca = Codec.encodeStringU16(g.getRaca());
            byte[] b1 = Codec.encodeTriBoolean(g.isSeDaBemComCachorros());
            byte[] b2 = Codec.encodeTriBoolean(g.isSeDaBemComGatos());
            byte[] b3 = Codec.encodeTriBoolean(g.isSeDaBemComCriancas());
            byte[] b4 = Codec.encodeTriBoolean(g.isAcessoExterior());
            byte[] b5 = Codec.encodeTriBoolean(g.isPossuiTelamento());
            return Codec.concat(idOng, nome, nasc, sexo, porte, vac, desc, raca, b1, b2, b3, b4, b5);
        } else {
            throw new IllegalArgumentException("Tipo de animal não suportado: " + a.getClass());
        }
    }

    private Cachorro decodeCachorro(int id, byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<Integer> dIdOng = Codec.decodeInt(buf, off); off = dIdOng.nextOffset;
        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, off); off = dNome.nextOffset;
        Codec.Decoded<java.time.LocalDate> dNasc = Codec.decodeLocalDate(buf, off); off = dNasc.nextOffset;
        Codec.Decoded<Character> dSexo = Codec.decodeChar(buf, off); off = dSexo.nextOffset;
    Codec.Decoded<Porte> dPorte = Codec.decodeEnum(buf, off, Porte.class); off = dPorte.nextOffset;
    Codec.Decoded<Boolean> dVacinado = Codec.decodeTriBoolean(buf, off); off = dVacinado.nextOffset;
    Codec.Decoded<String> dDesc = Codec.decodeStringU16(buf, off); off = dDesc.nextOffset;

        Codec.Decoded<String> dRaca = Codec.decodeStringU16(buf, off); off = dRaca.nextOffset;
        Codec.Decoded<NivelAdestramento> dNivel = Codec.decodeEnum(buf, off, NivelAdestramento.class); off = dNivel.nextOffset;
        Codec.Decoded<Boolean> b1 = Codec.decodeTriBoolean(buf, off); off = b1.nextOffset;
        Codec.Decoded<Boolean> b2 = Codec.decodeTriBoolean(buf, off); off = b2.nextOffset;
        Codec.Decoded<Boolean> b3 = Codec.decodeTriBoolean(buf, off); off = b3.nextOffset;

        Cachorro c = new Cachorro();
    c.setId(id);
    c.setAtivo(tomb == 0);
    c.setIdOng(dIdOng.value);
    c.setNome(dNome.value);
    c.setDataNascimentoAprox(dNasc.value);
    c.setSexo(dSexo.value);
    c.setPorte(dPorte.value);
    c.setVacinado(Boolean.TRUE.equals(dVacinado.value));
    c.setDescricao(dDesc.value);

    c.setRaca(dRaca.value);
    c.setNivelAdestramento(dNivel.value);
    c.setSeDaBemComCachorros(Boolean.TRUE.equals(b1.value));
    c.setSeDaBemComGatos(Boolean.TRUE.equals(b2.value));
    c.setSeDaBemComCriancas(Boolean.TRUE.equals(b3.value));
        return c;
    }

    private Gato decodeGato(int id, byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<Integer> dIdOng = Codec.decodeInt(buf, off); off = dIdOng.nextOffset;
        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, off); off = dNome.nextOffset;
        Codec.Decoded<java.time.LocalDate> dNasc = Codec.decodeLocalDate(buf, off); off = dNasc.nextOffset;
        Codec.Decoded<Character> dSexo = Codec.decodeChar(buf, off); off = dSexo.nextOffset;
    Codec.Decoded<Porte> dPorte = Codec.decodeEnum(buf, off, Porte.class); off = dPorte.nextOffset;
    Codec.Decoded<Boolean> dVacinado = Codec.decodeTriBoolean(buf, off); off = dVacinado.nextOffset;
    Codec.Decoded<String> dDesc = Codec.decodeStringU16(buf, off); off = dDesc.nextOffset;

        Codec.Decoded<String> dRaca = Codec.decodeStringU16(buf, off); off = dRaca.nextOffset;
        Codec.Decoded<Boolean> b1 = Codec.decodeTriBoolean(buf, off); off = b1.nextOffset;
        Codec.Decoded<Boolean> b2 = Codec.decodeTriBoolean(buf, off); off = b2.nextOffset;
        Codec.Decoded<Boolean> b3 = Codec.decodeTriBoolean(buf, off); off = b3.nextOffset;
        Codec.Decoded<Boolean> b4 = Codec.decodeTriBoolean(buf, off); off = b4.nextOffset;
        Codec.Decoded<Boolean> b5 = Codec.decodeTriBoolean(buf, off); off = b5.nextOffset;

        Gato g = new Gato();
    g.setId(id);
    g.setAtivo(tomb == 0);
    g.setIdOng(dIdOng.value);
    g.setNome(dNome.value);
    g.setDataNascimentoAprox(dNasc.value);
    g.setSexo(dSexo.value);
    g.setPorte(dPorte.value);
    g.setVacinado(Boolean.TRUE.equals(dVacinado.value));
    g.setDescricao(dDesc.value);

    g.setRaca(dRaca.value);
    g.setSeDaBemComCachorros(Boolean.TRUE.equals(b1.value));
    g.setSeDaBemComGatos(Boolean.TRUE.equals(b2.value));
    g.setSeDaBemComCriancas(Boolean.TRUE.equals(b3.value));
    g.setAcessoExterior(Boolean.TRUE.equals(b4.value));
    g.setPossuiTelamento(Boolean.TRUE.equals(b5.value));
        return g;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (bplus != null) bplus.close();
        } finally {
            super.close();
        }
    }

    private static Animal cloneForVacuum(Animal a) {
        // Clonagem simples para regravar no vacuum sem compartilhar referência
        if (a instanceof Cachorro c) {
            Cachorro n = new Cachorro();
            copiarBasico(a, n);
            n.setRaca(c.getRaca());
            n.setNivelAdestramento(c.getNivelAdestramento());
            n.setSeDaBemComCachorros(c.isSeDaBemComCachorros());
            n.setSeDaBemComGatos(c.isSeDaBemComGatos());
            n.setSeDaBemComCriancas(c.isSeDaBemComCriancas());
            return n;
        } else if (a instanceof Gato g) {
            Gato n = new Gato();
            copiarBasico(a, n);
            n.setRaca(g.getRaca());
            n.setSeDaBemComCachorros(g.isSeDaBemComCachorros());
            n.setSeDaBemComGatos(g.isSeDaBemComGatos());
            n.setSeDaBemComCriancas(g.isSeDaBemComCriancas());
            n.setAcessoExterior(g.isAcessoExterior());
            n.setPossuiTelamento(g.isPossuiTelamento());
            return n;
        }
        throw new IllegalArgumentException("Tipo de animal não suportado: " + a.getClass());
    }

    private static void copiarBasico(Animal from, Animal to) {
        to.setId(from.getId());
        to.setIdOng(from.getIdOng());
        to.setNome(from.getNome());
        to.setDataNascimentoAprox(from.getDataNascimentoAprox());
        to.setSexo(from.getSexo());
        to.setPorte(from.getPorte());
        to.setDescricao(from.getDescricao());
        to.setAtivo(from.isAtivo());
    }
}
