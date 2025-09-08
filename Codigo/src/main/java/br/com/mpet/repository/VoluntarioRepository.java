package br.com.mpet.repository;

import br.com.mpet.domain.Voluntario;
import br.com.mpet.storage.BinaryFileStore;
import br.com.mpet.storage.LZWCodec;
import br.com.mpet.storage.index.BPlusTreeIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class VoluntarioRepository implements AutoCloseable {
    private final BinaryFileStore store;
    private final BPlusTreeIndex index; // CPF -> address

    public VoluntarioRepository(Path dataDir) throws IOException {
    this.store = new BinaryFileStore(dataDir.resolve("voluntarios.dat"), new LZWCodec());
        this.index = new BPlusTreeIndex(dataDir.resolve("voluntarios.bpt"));
        rebuildIfEmpty();
    }

    private void rebuildIfEmpty() throws IOException {
        // If index empty but store has data, rebuild
        final List<String> keys = new ArrayList<>();
        store.forEach((pos, payload) -> {
            try {
                Voluntario v = Voluntario.fromBytes(payload);
                index.put(v.cpf, pos);
                keys.add(v.cpf);
            } catch (IOException e) { /* skip */ }
        });
    }

    public boolean create(Voluntario v) throws IOException {
        if (index.get(v.cpf).isPresent()) return false;
        long pos = store.append(v.toBytes());
        index.put(v.cpf, pos);
        return true;
    }

    public Optional<Voluntario> readByCpf(String cpf) throws IOException {
        Optional<Long> pos = index.get(cpf);
        if (pos.isEmpty()) return Optional.empty();
        return store.readAt(pos.get()).map(data -> {
            try { return Voluntario.fromBytes(data); } catch (IOException e) { return null; }
        });
    }

    public List<Voluntario> searchByNomeFragment(String frag) throws IOException {
        String f = frag.toLowerCase();
        List<Voluntario> out = new ArrayList<>();
        store.forEach((pos, payload) -> {
            Voluntario v = Voluntario.fromBytes(payload);
            String n = v.nome == null ? "" : v.nome.toLowerCase();
            String s = v.sobrenome == null ? "" : v.sobrenome.toLowerCase();
            if (br.com.mpet.algorithms.KMP.contains(n, f) || br.com.mpet.algorithms.KMP.contains(s, f)) out.add(v);
        });
        return out;
    }

    public boolean update(Voluntario v) throws IOException {
        Optional<Long> p = index.get(v.cpf);
        if (p.isEmpty()) return false;
        long newPos = store.updateAt(p.get(), v.toBytes());
        if (newPos != p.get()) index.put(v.cpf, newPos);
        return true;
    }

    public boolean delete(String cpf) throws IOException {
        Optional<Long> p = index.get(cpf);
        if (p.isEmpty()) return false;
        boolean ok = store.deleteAt(p.get());
        if (ok) index.remove(cpf);
        return ok;
    }

    @Override public void close() throws IOException { index.close(); store.close(); }

    public void vacuum() throws IOException {
        store.vacuum();
        // clear index
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Long> e : index.entries()) keys.add(e.getKey());
        for (String k : keys) index.remove(k);
        // rebuild
        store.forEach((pos, payload) -> {
            Voluntario v = Voluntario.fromBytes(payload);
            index.put(v.cpf, pos);
        });
    }
}
