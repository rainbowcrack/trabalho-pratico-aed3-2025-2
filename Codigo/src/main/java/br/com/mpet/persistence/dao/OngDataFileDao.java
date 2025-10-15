package br.com.mpet.persistence.dao;

import br.com.mpet.model.Ong;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.index.BPlusTreeIndex;
import br.com.mpet.persistence.io.Codec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OngDataFileDao extends BaseDataFile<Ong> implements OngDao {

    private static final int REC_POS_TOMBSTONE = 0;
    private static final int REC_POS_ID = 1;
    private static final int REC_POS_LEN = 5;
    private static final int REC_POS_PAYLOAD = 9;

    private final Map<Integer, Long> indexById = new HashMap<>();
    private final BPlusTreeIndex bplus;

    public OngDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        this.bplus = new BPlusTreeIndex(idxFile, versaoFormato, 64);
        rebuildIfEmpty();
    }

    @Override
    public synchronized Ong create(Ong entity) throws IOException {
        if (entity == null) throw new IllegalArgumentException("A entidade não pode ser nula.");
        entity.setId(nextIdAndIncrement());

        byte[] payload = encode(entity);
        byte[] record = montarRegistro((byte) 0, entity.getId(), payload);
        long offset = appendRecord(record);

        indexById.put(entity.getId(), offset);
        bplus.put(entity.getId(), offset);
        incrementCountAtivos();
        return entity;
    }

    @Override
    public synchronized Optional<Ong> read(Integer id) throws IOException {
        if (id == null) return Optional.empty();
        Long offset = indexById.get(id);
        if (offset == null) {
            offset = bplus.get(id);
            if (offset != null) indexById.put(id, offset);
        }
        if (offset == null) return Optional.empty();

        raf.seek(offset + REC_POS_TOMBSTONE);
        if (raf.readByte() == 1) {
            return Optional.empty(); // Tombstoned
        }

        return Optional.of(readAtOffset(offset));
    }

    @Override
    public synchronized boolean update(Ong entity) throws IOException {
        if (entity == null) return false;
        Long offset = indexById.get(entity.getId());
        if (offset == null) return false;

        raf.seek(offset + REC_POS_TOMBSTONE);
        if (raf.readByte() != 0) return false;

        raf.seek(offset + REC_POS_LEN);
        int oldLen = raf.readInt();
        byte[] newPayload = encode(entity);

        if (newPayload.length == oldLen) {
            overwritePayload(offset + REC_POS_PAYLOAD, newPayload);
        } else {
            markTombstone(offset);
            decrementCountAtivos();
            long newOffset = appendRecord(montarRegistro((byte) 0, entity.getId(), newPayload));
            indexById.put(entity.getId(), newOffset);
            bplus.put(entity.getId(), newOffset);
            incrementCountAtivos();
        }
        return true;
    }

    @Override
    public synchronized boolean delete(Integer id) throws IOException {
        if (id == null) return false;
        Long offset = indexById.get(id);
        if (offset == null) return false;

        raf.seek(offset + REC_POS_TOMBSTONE);
        if (raf.readByte() == 0) {
            markTombstone(offset);
            decrementCountAtivos();
        }
        indexById.remove(id);
        bplus.remove(id);
        return true;
    }

    @Override
    public synchronized List<Ong> listAllActive() throws IOException {
        List<Ong> list = new ArrayList<>();
        long len = raf.length();
        long pos = br.com.mpet.persistence.io.FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (tomb == 0) {
                list.add(readAtOffset(pos));
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        return list;
    }

    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexById.isEmpty()) return;
        indexById.clear();
        int ativos = 0;
        long len = raf.length();
        long pos = br.com.mpet.persistence.io.FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_ID);
            int id = raf.readInt();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (payloadLen < 0) break;
            if (tomb == 0) {
                indexById.put(id, pos);
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

    @Override
    public synchronized void vacuum() throws IOException {
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (OngDataFileDao tempDao = new OngDataFileDao(tempFile, this.versaoFormato)) {
            for (Ong ong : listAllActive()) {
                tempDao.create(cloneForVacuum(ong));
            }
        }
        this.close();
        
        File mainIdx = new File(file.getParentFile(), file.getName() + ".idx");
        File tempIdx = new File(tempFile.getParentFile(), tempFile.getName() + ".idx");

        if (!file.delete() || !tempFile.renameTo(file)) {
            throw new IOException("Falha ao substituir o arquivo de dados principal.");
        }
        if (mainIdx.exists() && !mainIdx.delete()) {
             throw new IOException("Falha ao apagar o índice antigo.");
        }
        if (tempIdx.exists() && !tempIdx.renameTo(mainIdx)) {
            throw new IOException("Falha ao renomear o índice temporário.");
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (bplus != null) bplus.close();
        } finally {
            super.close();
        }
    }

    private byte[] montarRegistro(byte tombstone, int id, byte[] payload) {
        byte[] idb = Codec.encodeInt(id);
        byte[] lenb = Codec.encodeInt(payload.length);
        byte[] header = new byte[]{ tombstone, idb[0], idb[1], idb[2], idb[3], lenb[0], lenb[1], lenb[2], lenb[3] };
        return Codec.concat(header, payload);
    }

    private Ong readAtOffset(long offset) throws IOException {
        raf.seek(offset + REC_POS_ID);
        int id = raf.readInt();
        raf.seek(offset + REC_POS_LEN);
        int payloadLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payloadLen);
        return decode(id, buf);
    }

    private byte[] encode(Ong ong) {
        byte[] nome = Codec.encodeStringU16(ong.getNome());
        byte[] cnpj = Codec.encodeStringU16(ong.getCnpj());
        byte[] endereco = Codec.encodeStringU16(ong.getEndereco());
        byte[] telefone = Codec.encodeStringU16(ong.getTelefone());
        byte[] idResponsavel = Codec.encodeInt(ong.getIdResponsavel());
        return Codec.concat(nome, cnpj, endereco, telefone, idResponsavel);
    }

    private Ong decode(int id, byte[] buf) {
        int offset = 0;
        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, offset); offset = dNome.nextOffset;
        Codec.Decoded<String> dCnpj = Codec.decodeStringU16(buf, offset); offset = dCnpj.nextOffset;
        Codec.Decoded<String> dEndereco = Codec.decodeStringU16(buf, offset); offset = dEndereco.nextOffset;
        Codec.Decoded<String> dTelefone = Codec.decodeStringU16(buf, offset); offset = dTelefone.nextOffset;
        Codec.Decoded<Integer> dIdResponsavel = Codec.decodeInt(buf, offset);

        Ong ong = new Ong();
        ong.setId(id);
        ong.setNome(dNome.value);
        ong.setCnpj(dCnpj.value);
        ong.setEndereco(dEndereco.value);
        ong.setTelefone(dTelefone.value);
        ong.setIdResponsavel(dIdResponsavel.value);
        return ong;
    }
    
    private Ong cloneForVacuum(Ong ong) {
        Ong clone = new Ong();
        clone.setId(ong.getId());
        clone.setNome(ong.getNome());
        clone.setCnpj(ong.getCnpj());
        clone.setEndereco(ong.getEndereco());
        clone.setTelefone(ong.getTelefone());
        clone.setIdResponsavel(ong.getIdResponsavel());
        return clone;
    }
}
