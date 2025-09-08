package br.com.mpet.repository;

import br.com.mpet.domain.Ong;
import br.com.mpet.storage.BinaryFileStore;
import br.com.mpet.storage.LZWCodec;
import br.com.mpet.storage.index.BPlusTreeIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class OngRepository implements AutoCloseable {
    private final BinaryFileStore store;
    private final BPlusTreeIndex index; // nome -> address

    public OngRepository(Path dataDir) throws IOException {
    this.store = new BinaryFileStore(dataDir.resolve("ongs.dat"), new LZWCodec());
        this.index = new BPlusTreeIndex(dataDir.resolve("ongs.bpt"));
        rebuildIfEmpty();
    }

    private void rebuildIfEmpty() throws IOException {
        store.forEach((pos, payload) -> {
            try {
                Ong o = Ong.fromBytes(payload);
                index.put(o.nome, pos);
            } catch (IOException e) { /* skip */ }
        });
    }

    public boolean create(Ong o) throws IOException {
        if (index.get(o.nome).isPresent()) return false;
        long pos = store.append(o.toBytes());
        index.put(o.nome, pos);
        return true;
    }

    public Optional<Ong> readByNome(String nome) throws IOException {
        Optional<Long> p = index.get(nome);
        if (p.isEmpty()) return Optional.empty();
        return store.readAt(p.get()).map(data -> {
            try { return Ong.fromBytes(data); } catch (IOException e) { return null; }
        });
    }

    public boolean update(Ong o) throws IOException {
        Optional<Long> p = index.get(o.nome);
        if (p.isEmpty()) return false;
        long newPos = store.updateAt(p.get(), o.toBytes());
        if (newPos != p.get()) index.put(o.nome, newPos);
        return true;
    }

    public boolean delete(String nome) throws IOException {
        Optional<Long> p = index.get(nome);
        if (p.isEmpty()) return false;
        boolean ok = store.deleteAt(p.get());
        if (ok) index.remove(nome);
        return ok;
    }

    public List<Ong> listOrdered() throws IOException {
        List<Ong> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : index.entries()) {
            store.readAt(e.getValue()).ifPresent(data -> {
                try { out.add(Ong.fromBytes(data)); } catch (IOException ex) { /* skip */ }
            });
        }
        return out;
    }

    @Override public void close() throws IOException { index.close(); store.close(); }

    public void vacuum() throws IOException {
        store.vacuum();
        // clear and rebuild index
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Long> e : index.entries()) keys.add(e.getKey());
        for (String k : keys) index.remove(k);
        store.forEach((pos, payload) -> {
            try {
                Ong o = Ong.fromBytes(payload);
                index.put(o.nome, pos);
            } catch (IOException e) { /* skip */ }
        });
    }
}
