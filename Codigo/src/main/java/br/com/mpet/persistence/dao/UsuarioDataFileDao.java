package br.com.mpet.persistence.dao;

import br.com.mpet.model.*;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import br.com.mpet.persistence.index.ArvoreElemento;
import br.com.mpet.persistence.index.BTree;
import br.com.mpet.persistence.io.Codec;
import br.com.mpet.persistence.io.FileHeaderHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * DAO genérico para entidades do tipo Usuario (Adotante e Voluntario).
 * Chave primária lógica: CPF (String). O índice B+ é baseado num idKey int derivado do CPF
 * (hash estável), com verificação do CPF exato por um mapa em memória.
 *
 * Layout do registro binário:
 * [0]   tipo        (1 byte)  1=ADOTANTE 2=VOLUNTARIO
 * [1]   tombstone   (1 byte)  0=ativo, 1=removido
 * [2-5] idKey       (4 bytes) int derivado do CPF (hash estável)
 * [6-7] tamCPF      (2 bytes) tamanho da string CPF (U16, usa Codec)
 * [8..?] payload    (bytes)   dados específicos do tipo + campos de Usuario
 *
 * Observação: armazenamos o CPF no payload como StringU16 (além do idKey) para
 * checagem de colisões de hash e para leitura do valor original.
 */
public class UsuarioDataFileDao<T extends Usuario> extends BaseDataFile<T> implements CrudDao<T, String> {

    // Tipos
    private static final byte TIPO_ADOTANTE = 1;
    private static final byte TIPO_VOLUNTARIO = 2;

    // Offsets
    private static final int REC_POS_TIPO = 0;
    private static final int REC_POS_TOMBSTONE = 1;
    private static final int REC_POS_IDKEY = 2;   // int
    private static final int REC_POS_LEN = 6;     // int tamanho total do payload (inclui CPF codificado + campos)
    private static final int REC_POS_PAYLOAD = 10;

    private final Class<T> type;
    private final Map<String, Long> indexByCpf = new HashMap<>();
    private BTree<ArvoreElemento> bplus;

    public UsuarioDataFileDao(File file, byte versaoFormato, Class<T> type) throws IOException {
        super(file, versaoFormato);
        this.type = type;
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        try {
            this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath());
        } catch (NoSuchMethodException e) {
            throw new IOException("Falha ao inicializar BTree de usuários", e);
        }
        rebuildIfEmpty();
    }

    @Override
    public synchronized T create(T entity) throws IOException {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entity.getCpf(), "CPF requerido");
        if (!entity.isAtivo()) entity.setAtivo(true);

        int idKey = cpfKey(entity.getCpf());
        byte tipo = resolveTipo(entity);
        byte[] payload = encodeUsuario(entity);
        byte[] full = montarRegistro(tipo, (byte)0, idKey, payload);
        long off = appendRecord(full);

        indexByCpf.put(entity.getCpf(), off);
        try { bplus.create(new ArvoreElemento(idKey, off)); } catch (Exception e) { throw new IOException(e); }
        incrementCountAtivos();
        return entity;
    }

    @Override
    public synchronized Optional<T> read(String cpf) throws IOException {
        if (cpf == null) return Optional.empty();
        Long off = indexByCpf.get(cpf);
        if (off == null) {
            int idKey = cpfKey(cpf);
            try {
                ArvoreElemento el = bplus.read(idKey);
                if (el != null) off = el.getAddress();
            } catch (Exception ignore) { }
        }
        if (off != null) {
            T u = readAtOffset(off);
            if (u != null && cpf.equals(u.getCpf())) {
                indexByCpf.put(cpf, off);
                return Optional.of(u);
            }
        }
        // Fallback: varre o arquivo e procura pelo CPF exato (trata possíveis colisões de hash)
        Long scanned = scanOffsetByCpf(cpf);
        if (scanned != null) {
            T u2 = readAtOffset(scanned);
            if (u2 != null) {
                indexByCpf.put(cpf, scanned);
                // Não atualiza BTree em caso de colisão para evitar sobrescrever outra entrada
                return Optional.of(u2);
            }
        }
        return Optional.empty();
    }

    private Long scanOffsetByCpf(String cpf) throws IOException {
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (payloadLen < 0) break;
            if (tomb == 0) {
                byte[] buf = readBytes(pos + REC_POS_PAYLOAD, payloadLen);
                Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, 0);
                if (cpf.equals(dCpf.value)) return pos;
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        return null;
    }

    @Override
    public synchronized boolean update(T entity) throws IOException {
        if (entity == null || entity.getCpf() == null) return false;
        Long off = indexByCpf.get(entity.getCpf());
        if (off == null) return false;

        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb != 0) return false;
        raf.seek(off + REC_POS_LEN);
        int oldLen = raf.readInt();

        byte[] newPayload = encodeUsuario(entity);
        if (newPayload.length == oldLen) {
            overwritePayload(off + REC_POS_PAYLOAD, newPayload);
            return true;
        } else {
            markTombstone(off);
            decrementCountAtivos();
            int idKey = cpfKey(entity.getCpf());
            byte tipo = resolveTipo(entity);
            byte[] full = montarRegistro(tipo, (byte)0, idKey, newPayload);
            long newOff = appendRecord(full);
            indexByCpf.put(entity.getCpf(), newOff);
            try { bplus.update(idKey, newOff); } catch (Exception e) { throw new IOException(e); }
            incrementCountAtivos();
            return true;
        }
    }

    @Override
    public synchronized boolean delete(String cpf) throws IOException {
        if (cpf == null) return false;
        Long off = indexByCpf.get(cpf);
        if (off == null) return false;
        raf.seek(off + REC_POS_TOMBSTONE);
        byte tomb = raf.readByte();
        if (tomb == 0) {
            markTombstone(off);
            decrementCountAtivos();
        }
        indexByCpf.remove(cpf);
        try { bplus.delete(cpfKey(cpf)); } catch (Exception e) { throw new IOException(e); }
        return true;
    }

    @Override
    public synchronized List<T> listAllActive() throws IOException {
        List<T> out = new ArrayList<>();
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos < len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (tomb == 0) {
                T u = readAtOffset(pos);
                if (u != null) out.add(u);
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        return out;
    }

    @Override
    public synchronized void rebuildIfEmpty() throws IOException {
        if (!indexByCpf.isEmpty()) return;
        indexByCpf.clear();
        // reset índice em disco
        bplus.close();
        File idxFile = new File(file.getParentFile(), file.getName() + ".idx");
        if (idxFile.exists()) idxFile.delete();
        try { this.bplus = new BTree<>(ArvoreElemento.class.getConstructor(), 4, idxFile.getPath()); }
        catch (Exception e) { throw new IOException(e); }

        int ativos = 0;
        long len = raf.length();
        long pos = FileHeaderHelper.HEADER_SIZE;
        while (pos + REC_POS_PAYLOAD <= len) {
            raf.seek(pos + REC_POS_TOMBSTONE);
            byte tomb = raf.readByte();
            raf.seek(pos + REC_POS_IDKEY);
            int idKey = raf.readInt();
            raf.seek(pos + REC_POS_LEN);
            int payloadLen = raf.readInt();
            if (payloadLen < 0) break;
            if (tomb == 0) {
                // precisamos extrair o CPF para popular o mapa por chave real
                byte[] buf = readBytes(pos + REC_POS_PAYLOAD, payloadLen);
                Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, 0);
                String cpf = dCpf.value;
                indexByCpf.put(cpf, pos);
                try { bplus.create(new ArvoreElemento(idKey, pos)); } catch (Exception e) { throw new IOException(e); }
                ativos++;
            }
            pos += REC_POS_PAYLOAD + payloadLen;
        }
        if (ativos != header.countAtivos) { header.countAtivos = ativos; persistHeader(); }
    }

    @Override
    public synchronized void vacuum() throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (UsuarioDataFileDao<T> novo = new UsuarioDataFileDao<>(temp, this.versaoFormato, this.type)) {
            for (T u : listAllActive()) novo.create(u);
        }
        this.close();
        if (!file.delete()) throw new IOException("Falha ao apagar antigo: "+file);
        if (!temp.renameTo(file)) throw new IOException("Falha ao renomear temp: "+temp);
        // trocar índice .idx correspondente
        File mainIdx = new File(file.getParentFile(), file.getName() + ".idx");
        File tempIdx = new File(temp.getParentFile(), temp.getName() + ".idx");
        if (tempIdx.exists()) {
            if (mainIdx.exists() && !mainIdx.delete()) throw new IOException("Falha ao apagar idx antigo: "+mainIdx);
            if (!tempIdx.renameTo(mainIdx)) throw new IOException("Falha ao renomear idx temp: "+tempIdx);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try { if (bplus != null) bplus.close(); } finally { super.close(); }
    }

    // ========================= Helpers =========================
    private static int cpfKey(String cpf) {
        // normaliza removendo não dígitos; usa hashCode estável do String normalizado
        String norm = cpf.replaceAll("\\D", "");
        return norm.hashCode();
    }

    private static byte resolveTipo(Usuario u) {
        if (u instanceof Adotante) return TIPO_ADOTANTE;
        if (u instanceof Voluntario) return TIPO_VOLUNTARIO;
        throw new IllegalArgumentException("Tipo de usuário não suportado: "+u.getClass());
    }

    private byte[] montarRegistro(byte tipo, byte tombstone, int idKey, byte[] payload) {
        byte[] idb = Codec.encodeInt(idKey);
        byte[] lenb = Codec.encodeInt(payload.length);
        byte[] header = new byte[]{ tipo, tombstone, idb[0], idb[1], idb[2], idb[3], lenb[0], lenb[1], lenb[2], lenb[3] };
        return Codec.concat(header, payload);
    }

    private T readAtOffset(long offset) throws IOException {
        raf.seek(offset + REC_POS_TIPO); int tipo = raf.readUnsignedByte();
        raf.seek(offset + REC_POS_TOMBSTONE); byte tomb = raf.readByte();
        raf.seek(offset + REC_POS_LEN); int payloadLen = raf.readInt();
        byte[] buf = readBytes(offset + REC_POS_PAYLOAD, payloadLen);
        switch (tipo) {
            case TIPO_ADOTANTE -> {
                Adotante a = decodeAdotante(tomb, buf);
                if (type.isInstance(a)) return type.cast(a);
                return null;
            }
            case TIPO_VOLUNTARIO -> {
                Voluntario v = decodeVoluntario(tomb, buf);
                if (type.isInstance(v)) return type.cast(v);
                return null;
            }
            default -> { return null; }
        }
    }

    private byte[] encodeUsuario(T u) {
    // prefixa CPF e campos comuns
        byte[] senha = Codec.encodeStringU16(u.getSenha());
        byte[] telefone = Codec.encodeStringU16(u.getTelefone());
        byte[] ativo = Codec.encodeTriBoolean(u.isAtivo()); // apenas como espelho, leitura usa tomb

        if (u instanceof Adotante a) {
            return Codec.concat(Codec.encodeStringU16(a.getCpf()), senha, telefone, ativo,
                    Codec.encodeStringU16(a.getNomeCompleto()),
                    Codec.encodeLocalDate(a.getDataNascimento()),
                    Codec.encodeEnum(a.getTipoMoradia()),
                    Codec.encodeTriBoolean(a.isPossuiTelaProtetora()),
                    Codec.encodeTriBoolean(a.isPossuiOutrosAnimais()),
                    Codec.encodeStringU16(a.getDescOutrosAnimais()),
                    Codec.encodeInt(a.getHorasForaDeCasa()),
                    Codec.encodeEnum(a.getComposicaoFamiliar()),
                    Codec.encodeTriBoolean(a.isViagensFrequentes()),
                    Codec.encodeStringU16(a.getDescViagensFrequentes()),
                    Codec.encodeTriBoolean(a.isJaTevePets()),
                    Codec.encodeStringU16(a.getExperienciaComPets()),
                    Codec.encodeStringU16(a.getMotivoAdocao()),
                    Codec.encodeTriBoolean(a.isCientePossuiResponsavel()),
                    Codec.encodeTriBoolean(a.isCienteCustos())
            );
        } else if (u instanceof Voluntario v) {
            return Codec.concat(Codec.encodeStringU16(v.getCpf()), senha, telefone, ativo,
                    Codec.encodeStringU16(v.getNome()),
                    Codec.encodeStringU16(v.getEndereco()),
                    Codec.encodeInt(v.getIdOng()),
                    Codec.encodeEnum(v.getCargo())
            );
        }
        throw new IllegalArgumentException("Usuário não suportado: "+u.getClass());
    }

    private Adotante decodeAdotante(byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, off); off = dCpf.nextOffset;
        Codec.Decoded<String> dSenha = Codec.decodeStringU16(buf, off); off = dSenha.nextOffset;
        Codec.Decoded<String> dTel = Codec.decodeStringU16(buf, off); off = dTel.nextOffset;
        Codec.Decoded<Boolean> dAtivo = Codec.decodeTriBoolean(buf, off); off = dAtivo.nextOffset;

        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, off); off = dNome.nextOffset;
        Codec.Decoded<java.time.LocalDate> dNasc = Codec.decodeLocalDate(buf, off); off = dNasc.nextOffset;
        Codec.Decoded<TipoMoradia> dMoradia = Codec.decodeEnum(buf, off, TipoMoradia.class); off = dMoradia.nextOffset;
        Codec.Decoded<Boolean> bTela = Codec.decodeTriBoolean(buf, off); off = bTela.nextOffset;
        Codec.Decoded<Boolean> bOutros = Codec.decodeTriBoolean(buf, off); off = bOutros.nextOffset;
        Codec.Decoded<String> dDescOutros = Codec.decodeStringU16(buf, off); off = dDescOutros.nextOffset;
        Codec.Decoded<Integer> dHoras = Codec.decodeInt(buf, off); off = dHoras.nextOffset;
        Codec.Decoded<ComposicaoFamiliar> dComp = Codec.decodeEnum(buf, off, ComposicaoFamiliar.class); off = dComp.nextOffset;
        Codec.Decoded<Boolean> dViagens = Codec.decodeTriBoolean(buf, off); off = dViagens.nextOffset;
        Codec.Decoded<String> dDescViagens = Codec.decodeStringU16(buf, off); off = dDescViagens.nextOffset;
        Codec.Decoded<Boolean> dJaTeve = Codec.decodeTriBoolean(buf, off); off = dJaTeve.nextOffset;
        Codec.Decoded<String> dExp = Codec.decodeStringU16(buf, off); off = dExp.nextOffset;
        Codec.Decoded<String> dMotivo = Codec.decodeStringU16(buf, off); off = dMotivo.nextOffset;
        Codec.Decoded<Boolean> dResp = Codec.decodeTriBoolean(buf, off); off = dResp.nextOffset;
        Codec.Decoded<Boolean> dCustos = Codec.decodeTriBoolean(buf, off); off = dCustos.nextOffset;

        Adotante a = new Adotante();
        a.setCpf(dCpf.value);
        a.setSenha(dSenha.value);
        a.setTelefone(dTel.value);
        a.setAtivo(tomb == 0);
        a.setNomeCompleto(dNome.value);
        a.setDataNascimento(dNasc.value);
        a.setTipoMoradia(dMoradia.value);
        a.setPossuiTelaProtetora(Boolean.TRUE.equals(bTela.value));
        a.setPossuiOutrosAnimais(Boolean.TRUE.equals(bOutros.value));
        a.setDescOutrosAnimais(dDescOutros.value);
        a.setHorasForaDeCasa(dHoras.value);
        a.setComposicaoFamiliar(dComp.value);
        a.setViagensFrequentes(Boolean.TRUE.equals(dViagens.value));
        a.setDescViagensFrequentes(dDescViagens.value);
        a.setJaTevePets(Boolean.TRUE.equals(dJaTeve.value));
        a.setExperienciaComPets(dExp.value);
        a.setMotivoAdocao(dMotivo.value);
        a.setCientePossuiResponsavel(Boolean.TRUE.equals(dResp.value));
        a.setCienteCustos(Boolean.TRUE.equals(dCustos.value));
        return a;
    }

    private Voluntario decodeVoluntario(byte tomb, byte[] buf) {
        int off = 0;
        Codec.Decoded<String> dCpf = Codec.decodeStringU16(buf, off); off = dCpf.nextOffset;
        Codec.Decoded<String> dSenha = Codec.decodeStringU16(buf, off); off = dSenha.nextOffset;
        Codec.Decoded<String> dTel = Codec.decodeStringU16(buf, off); off = dTel.nextOffset;
        Codec.Decoded<Boolean> dAtivo = Codec.decodeTriBoolean(buf, off); off = dAtivo.nextOffset;

        Codec.Decoded<String> dNome = Codec.decodeStringU16(buf, off); off = dNome.nextOffset;
        Codec.Decoded<String> dEnd = Codec.decodeStringU16(buf, off); off = dEnd.nextOffset;
        Codec.Decoded<Integer> dOng = Codec.decodeInt(buf, off); off = dOng.nextOffset;
        Codec.Decoded<Role> dCargo = Codec.decodeEnum(buf, off, Role.class); off = dCargo.nextOffset;

        Voluntario v = new Voluntario();
        v.setCpf(dCpf.value);
        v.setSenha(dSenha.value);
        v.setTelefone(dTel.value);
        v.setAtivo(tomb == 0);
        v.setNome(dNome.value);
        v.setEndereco(dEnd.value);
        v.setIdOng(dOng.value);
        v.setCargo(dCargo.value);
        return v;
    }
}
