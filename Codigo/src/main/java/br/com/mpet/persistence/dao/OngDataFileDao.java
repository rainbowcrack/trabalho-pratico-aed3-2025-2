package br.com.mpet.persistence.dao;

import br.com.mpet.model.Ong;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import br.com.mpet.persistence.index.ArvoreElemento;
import br.com.mpet.persistence.index.BTree;
import br.com.mpet.persistence.io.Codec;
import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OngDataFileDao extends BaseDataFile<Ong> implements CrudDao<Ong, Integer> {

    private static final int REC_POS_TOMBSTONE = 0; // +0
    private static final int REC_POS_ID = 1;        // +1..+4 (int)
    private static final int REC_POS_LEN = 5;       // +5..+8 (int)
    private static final int REC_POS_PAYLOAD = 9;   // +9..+9+len-1

    private final Map<Integer, Long> indexById = new HashMap<>();
    private BTree<ArvoreElemento> bplus;

    public OngDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        try {
            this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath());
        } catch (NoSuchMethodException e) {
            throw new IOException("Falha ao inicializar o índice BTree: construtor não encontrado.", e);
        }
        rebuildIfEmpty();
    }

    @Override
    public synchronized Ong create(Ong entity) throws IOException {
        if (entity == null) throw new IllegalArgumentException("entity == null");
        if (!entity.isAtivo()) entity.setAtivo(true);

        entity.setId(nextIdAndIncrement());

        byte[] payload = encodeOng(entity);
        byte[] full = montarRegistro((byte) 0, entity.getId(), payload);
        long offset = appendRecord(full);

        indexById.put(entity.getId(), offset);
        try {
            bplus.create(new ArvoreElemento(entity.getId(), offset));
        } catch (Exception e) {
            throw new IOException("Erro ao inserir no índice B+", e);
        }
        incrementCountAtivos();
        return entity;
    }

    @Override
    public synchronized Optional<Ong> read(Integer id) throws IOException {
        if (id == null) return Optional.empty();
        Long off = indexById.get(id);
        if (off == null) {
            try {
                ArvoreElemento el = bplus.read(id);
                if (el != null) off = el.getAddress();
            } catch (Exception e) { /* ignora */ }
        }
        if (off != null) indexById.put(id, off);
        if (off == null) return Optional.empty();
        Ong ong = readAtOffset(off);
        return Optional.ofNullable(ong);
    }

    @Override
    public synchronized boolean update(Ong entity) throws IOException {
        if (entity == null) return false;
        Long off = indexById.get(entity.getId());
        if (off == null) return false;

        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb != 0) return false;
        raf.seek(off + REC_POS_LEN);
        int oldLen = raf.readInt();

        byte[] newPayload = encodeOng(entity);
        if (newPayload.length == oldLen) {
            overwritePayload(off + REC_POS_PAYLOAD, newPayload);
            return true;
        } else {
            markTombstone(off);
            decrementCountAtivos();

            byte[] full = montarRegistro((byte) 0, entity.getId(), newPayload);
            long newOff = appendRecord(full);
            indexById.put(entity.getId(), newOff);
            try {
                bplus.update(entity.getId(), newOff);
            } catch (Exception e) {
                throw new IOException("Erro ao atualizar no índice B+", e);
            }
            incrementCountAtivos();
            return true;
        }
    }

    @Override
    public synchronized boolean delete(Integer id) throws IOException {
        if (id == null) return false;
        Long off = indexById.get(id);
        if (off == null) return false;

        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb == 0) {
            markTombstone(off);
            decrementCountAtivos();
        }
        indexById.remove(id);
        try {
            bplus.delete(id);
        } catch (Exception e) {
            throw new IOException("Erro ao deletar no índice B+", e);
        }
        return true;
    }

    @Override
    public synchronized List<Ong> listAllActive() throws IOException {
        List<Ong> list = new ArrayList<>();
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (tomb == 0) {
                Ong ong = readAtOffset(pos);
                if (ong != null) list.add(ong);
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        return list;
    }

    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexById.isEmpty()) return;
        indexById.clear();

        // Limpa o arquivo de índice B+ para reconstrução
        bplus.close();
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        if (idxFile.exists()) {
            idxFile.delete();
        } 
        try {
            this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath());
        } catch (NoSuchMethodException e) {
            throw new IOException("Falha ao reinicializar o índice BTree: construtor não encontrado.", e);
        }

        int ativos = 0;
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_ID);
            int id = raf.readInt();
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (payloadLen < 0) break;
            if (tomb == 0) {
                indexById.put(id, pos);
                try {
                    bplus.create(new ArvoreElemento(id, pos));
                } catch (Exception e) {
                    throw new IOException("Erro ao reconstruir índice B+", e);
                }
                ativos++;
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        if (ativos != header.countAtivos) {
            header.countAtivos = ativos;
            persistHeader();
        }
    }

    @Override
    public synchronized void vacuum() throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (OngDataFileDao novo = new OngDataFileDao(temp, this.versaoFormato)) {
            for (Ong ong : listAllActive()) {
                novo.create(ong);
            }
        }
        this.close();

        if (!file.delete()) throw new IOException("Falha ao apagar arquivo antigo: " + file);
        if (!temp.renameTo(file)) throw new IOException("Falha ao renomear arquivo temporário: " + temp);

        File mainIdx = new File(file.getParentFile(), file.getName() + ".idx");
        File tempIdx = new File(temp.getParentFile(), temp.getName() + ".idx");
        if (tempIdx.exists()) {
            if (mainIdx.exists() && !mainIdx.delete()) {
                throw new IOException("Falha ao apagar índice antigo: " + mainIdx);
            }
            if (!tempIdx.renameTo(mainIdx)) {
                throw new IOException("Falha ao renomear índice temporário: " + tempIdx);
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (bplus != null) bplus.close();
        } finally {
            super.close();
        }
    }

    private byte[] montarRegistro(byte tombstone, int id, byte[] payload) {
        byte[] idb = Codec.encodeInt(id);
        byte[] lenb = Codec.encodeInt(payload.length);
        return Codec.concat(new byte[]{tombstone}, idb, lenb, payload);
    }

    private Ong readAtOffset(long offset) throws IOException {
        raf.seek(offset + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        raf.seek(offset + REC_POS_ID);
        int id = raf.readInt();
        raf.seek(offset + REC_POS_LEN);
        int payloadLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payloadLen);
        return decodeOng(id, tomb, buf);
    }

    private byte[] encodeOng(Ong ong) {
    return Codec.concat(
        Codec.encodeStringU16(ong.getNome()),
        Codec.encodeStringU16(ong.getCnpj()),
        Codec.encodeStringU16(ong.getEndereco()),
        Codec.encodeStringU16(ong.getTelefone()),
        Codec.encodeStringU16(ong.getCpfResponsavel())
    );
    }

    private Ong decodeOng(int id, byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, off); off = dNome.nextOffset;
        Codec.Decoded<String> dCnpj = Codec.decodeStringU16(buf, off); off = dCnpj.nextOffset;
        Codec.Decoded<String> dEndereco = Codec.decodeStringU16(buf, off); off = dEndereco.nextOffset;
        Codec.Decoded<String> dTelefone = Codec.decodeStringU16(buf, off); off = dTelefone.nextOffset;
    Codec.Decoded<String> dCpfResp = Codec.decodeStringU16(buf, off);

        Ong ong = new Ong();
        ong.setId(id);
        ong.setAtivo(tomb == 0);
        ong.setNome(dNome.value);
        ong.setCnpj(dCnpj.value);
        ong.setEndereco(dEndereco.value);
        ong.setTelefone(dTelefone.value);
        ong.setCpfResponsavel(dCpfResp.value);
        return ong;
    }

}