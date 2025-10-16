package br.com.mpet.persistence.dao;

import br.com.mpet.model.Adocao;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import br.com.mpet.persistence.io.Codec;
import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * DAO para relação Adocao (Adotante CPF -> Animal ID) usando arquivo binário.
 */
public class AdocaoDataFileDao extends BaseDataFile<Adocao> implements CrudDao<Adocao, Integer> {

    private static final int REC_POS_TOMBSTONE = 0; // +0
    private static final int REC_POS_ID = 1;        // +1..+4 (int)
    private static final int REC_POS_LEN = 5;       // +5..+8 (int)
    private static final int REC_POS_PAYLOAD = 9;   // +9..+9+len-1

    private final Map<Integer, Long> indexById = new HashMap<>();

    public AdocaoDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
        rebuildIfEmpty();
    }

    @Override
    public synchronized Adocao create(Adocao entity) throws IOException {
        if (entity == null) throw new IllegalArgumentException("entity == null");
        if (!entity.isAtivo()) entity.setAtivo(true);
        entity.setId(nextIdAndIncrement());
        byte[] payload = encode(entity);
        byte[] full = montarRegistro((byte)0, entity.getId(), payload);
        long off = appendRecord(full);
        indexById.put(entity.getId(), off);
        incrementCountAtivos();
        return entity;
    }

    @Override
    public synchronized Optional<Adocao> read(Integer id) throws IOException {
        if (id == null) return Optional.empty();
        Long off = indexById.get(id);
        if (off == null) return Optional.empty();
        return Optional.ofNullable(readAt(off));
    }

    @Override
    public synchronized boolean update(Adocao e) throws IOException {
        if (e == null) return false;
        Long off = indexById.get(e.getId());
        if (off == null) return false;
        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb != 0) return false;
        raf.seek(off + REC_POS_LEN);
        int oldLen = raf.readInt();
        byte[] newPayload = encode(e);
        if (newPayload.length == oldLen) {
            overwritePayload(off + REC_POS_PAYLOAD, newPayload);
        } else {
            markTombstone(off); decrementCountAtivos();
            long novo = appendRecord(montarRegistro((byte)0, e.getId(), newPayload));
            indexById.put(e.getId(), novo); incrementCountAtivos();
        }
        return true;
    }

    @Override
    public synchronized boolean delete(Integer id) throws IOException {
        if (id == null) return false;
        Long off = indexById.get(id);
        if (off == null) return false;
        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb == 0) { markTombstone(off); decrementCountAtivos(); }
        indexById.remove(id);
        return true;
    }

    @Override
    public synchronized List<Adocao> listAllActive() throws IOException {
        List<Adocao> list = new ArrayList<>();
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int pay = raf.readInt();
            if (tomb == 0) {
                Adocao a = readAt(pos);
                if (a != null) list.add(a);
            }
            pos += REC_POS_PAYLOAD + pay;
        }
        return list;
    }

    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexById.isEmpty()) return;
        indexById.clear();
        int ativos = 0;
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_ID);
            int id = raf.readInt();
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int pay = raf.readInt();
            if (pay < 0) break;
            if (tomb == 0) { indexById.put(id, pos); ativos++; }
            pos += REC_POS_PAYLOAD + pay;
        }
        if (ativos != header.countAtivos) { header.countAtivos = ativos; persistHeader(); }
    }

    @Override
    public synchronized void vacuum() throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (AdocaoDataFileDao novo = new AdocaoDataFileDao(temp, this.versaoFormato)) {
            for (Adocao a : listAllActive()) novo.create(a);
        }
        this.close();
        if (!file.delete()) throw new IOException("Falha ao apagar arquivo antigo: " + file);
        if (!temp.renameTo(file)) throw new IOException("Falha ao renomear arquivo temporário: " + temp);
    }

    private byte[] montarRegistro(byte tomb, int id, byte[] payload) {
        return Codec.concat(new byte[]{ tomb }, Codec.encodeInt(id), Codec.encodeInt(payload.length), payload);
    }

    private Adocao readAt(long offset) throws IOException {
        raf.seek(offset + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        raf.seek(offset + REC_POS_ID);
        int id = raf.readInt();
        raf.seek(offset + REC_POS_LEN);
        int payLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payLen);
        return decode(id, tomb, buf);
    }

    private byte[] encode(Adocao a) {
        return Codec.concat(
                Codec.encodeStringU16(a.getCpfAdotante()),
                Codec.encodeInt(a.getIdAnimal()),
                Codec.encodeLocalDate(a.getDataAdocao())
        );
    }

    private Adocao decode(int id, byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, off); off = dCpf.nextOffset;
        Codec.Decoded<Integer> dAnimal = Codec.decodeInt(buf, off); off = dAnimal.nextOffset;
        Codec.Decoded<java.time.LocalDate> dData = Codec.decodeLocalDate(buf, off);
        Adocao a = new Adocao();
        a.setId(id); a.setAtivo(tomb == 0);
        a.setCpfAdotante(dCpf.value); a.setIdAnimal(dAnimal.value); a.setDataAdocao(dData.value);
        return a;
    }
}
