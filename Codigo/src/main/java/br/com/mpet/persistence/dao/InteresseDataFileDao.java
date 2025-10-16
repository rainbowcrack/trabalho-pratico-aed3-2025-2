package br.com.mpet.persistence.dao;

import br.com.mpet.model.Interesse;
import br.com.mpet.model.InteresseStatus;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import br.com.mpet.persistence.index.ArvoreElemento;
import br.com.mpet.persistence.index.BTree;
import br.com.mpet.persistence.io.Codec;
import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** DAO para registros de Interesse (adotante -> animal). */
public class InteresseDataFileDao extends BaseDataFile<Interesse> implements CrudDao<Interesse, Integer> {

    private static final int REC_POS_TOMBSTONE = 0;
    private static final int REC_POS_ID = 1;
    private static final int REC_POS_LEN = 5;
    private static final int REC_POS_PAYLOAD = 9;

    private final Map<Integer, Long> indexById = new HashMap<>();
    private BTree<ArvoreElemento> bplus;

    public InteresseDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        try { this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath()); }
        catch (NoSuchMethodException e) { throw new IOException("Falha ao inicializar BTree", e); }
        rebuildIfEmpty();
    }

    @Override
    public synchronized Interesse create(Interesse e) throws IOException {
        if (e == null) throw new IllegalArgumentException("entity == null");
        if (!e.isAtivo()) e.setAtivo(true);
        if (e.getStatus() == null) e.setStatus(InteresseStatus.PENDENTE);
        e.setId(nextIdAndIncrement());
        byte[] payload = encode(e);
        long off = appendRecord(montarRegistro((byte)0, e.getId(), payload));
        indexById.put(e.getId(), off);
        try { bplus.create(new ArvoreElemento(e.getId(), off)); } catch (Exception ex) { throw new IOException("Erro ao inserir no índice B+", ex); }
        incrementCountAtivos();
        return e;
    }

    @Override
    public synchronized Optional<Interesse> read(Integer id) throws IOException {
        Long off = indexById.get(id);
        if (off == null) {
            try { ArvoreElemento el = bplus.read(id); if (el != null) off = el.getAddress(); } catch (Exception ignore) {}
        }
        if (off == null) return Optional.empty();
        indexById.put(id, off);
        return Optional.ofNullable(readAt(off));
    }

    @Override
    public synchronized boolean update(Interesse e) throws IOException {
        Long off = indexById.get(e.getId());
        if (off == null) return false;
        raf.seek(off + REC_POS_TOMBSTONE);
        if (raf.readByte() != 0) return false;
        raf.seek(off + REC_POS_LEN);
        int oldLen = raf.readInt();
        byte[] newPayload = encode(e);
        if (newPayload.length == oldLen) {
            overwritePayload(off + REC_POS_PAYLOAD, newPayload);
        } else {
            markTombstone(off); decrementCountAtivos();
            long novo = appendRecord(montarRegistro((byte)0, e.getId(), newPayload));
            indexById.put(e.getId(), novo);
            try { bplus.update(e.getId(), novo); } catch (Exception ex) { throw new IOException("Erro ao atualizar no índice B+", ex); }
            incrementCountAtivos();
        }
        return true;
    }

    @Override
    public synchronized boolean delete(Integer id) throws IOException {
        Long off = indexById.get(id);
        if (off == null) return false;
        raf.seek(off + REC_POS_TOMBSTONE);
        if (raf.readByte() == 0) { markTombstone(off); decrementCountAtivos(); }
        indexById.remove(id);
        try { bplus.delete(id); } catch (Exception ex) { throw new IOException("Erro ao deletar no índice B+", ex); }
        return true;
    }

    @Override
    public synchronized List<Interesse> listAllActive() throws IOException {
        List<Interesse> list = new ArrayList<>();
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int pay = raf.readInt();
            if (tomb == 0) {
                Interesse it = readAt(pos);
                if (it != null) list.add(it);
            }
            pos += REC_POS_PAYLOAD + pay;
        }
        return list;
    }

    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexById.isEmpty()) return;
    indexById.clear();
    // reset idx
    if (bplus != null) bplus.close();
    File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
    if (idxFile.exists()) idxFile.delete();
    try { this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath()); }
    catch (Exception e) { throw new IOException("Falha ao reinicializar BTree", e); }
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
            if (tomb == 0) { 
                indexById.put(id, pos); 
                try { bplus.create(new ArvoreElemento(id, pos)); } catch (Exception e) { throw new IOException("Erro ao reconstruir índice B+", e); }
                ativos++; 
            }
            pos += REC_POS_PAYLOAD + pay;
        }
        if (ativos != header.countAtivos) { header.countAtivos = ativos; persistHeader(); }
    }

    @Override
    public synchronized void vacuum() throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (InteresseDataFileDao novo = new InteresseDataFileDao(temp, this.versaoFormato)) {
            for (Interesse a : listAllActive()) novo.create(a);
        }
        this.close();
        if (!file.delete()) throw new IOException("Falha ao apagar arquivo antigo: " + file);
        if (!temp.renameTo(file)) throw new IOException("Falha ao renomear arquivo temporário: " + temp);
        // troca idx
        File mainIdx = new File(file.getParentFile(), file.getName() + ".idx");
        File tempIdx = new File(temp.getParentFile(), temp.getName() + ".idx");
        if (tempIdx.exists()) {
            if (mainIdx.exists() && !mainIdx.delete()) throw new IOException("Falha ao apagar idx antigo: "+mainIdx);
            if (!tempIdx.renameTo(mainIdx)) throw new IOException("Falha ao renomear idx temp: "+tempIdx);
        }
    }

    private byte[] montarRegistro(byte tomb, int id, byte[] payload) {
        return Codec.concat(new byte[]{ tomb }, Codec.encodeInt(id), Codec.encodeInt(payload.length), payload);
    }

    private Interesse readAt(long offset) throws IOException {
        raf.seek(offset + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        raf.seek(offset + REC_POS_ID);
        int id = raf.readInt();
        raf.seek(offset + REC_POS_LEN);
        int payLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payLen);
        return decode(id, tomb, buf);
    }

    private byte[] encode(Interesse a) {
        return Codec.concat(
                Codec.encodeStringU16(a.getCpfAdotante()),
                Codec.encodeInt(a.getIdAnimal()),
                Codec.encodeLocalDate(a.getData()),
                Codec.encodeEnum(a.getStatus())
        );
    }

    private Interesse decode(int id, byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, off); off = dCpf.nextOffset;
        Codec.Decoded<Integer> dAnimal = Codec.decodeInt(buf, off); off = dAnimal.nextOffset;
        Codec.Decoded<java.time.LocalDate> dData = Codec.decodeLocalDate(buf, off); off = dData.nextOffset;
        Codec.Decoded<InteresseStatus> dStatus = Codec.decodeEnum(buf, off, InteresseStatus.class);
        Interesse a = new Interesse();
        a.setId(id); a.setAtivo(tomb == 0);
        a.setCpfAdotante(dCpf.value); a.setIdAnimal(dAnimal.value); a.setData(dData.value); a.setStatus(dStatus.value);
        return a;
    }

    @Override
    public synchronized void close() throws IOException {
        try { if (bplus != null) bplus.close(); } finally { super.close(); }
    }
}
