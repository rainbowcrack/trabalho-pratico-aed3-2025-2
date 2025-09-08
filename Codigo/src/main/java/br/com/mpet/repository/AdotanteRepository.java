package br.com.mpet.repository;

import br.com.mpet.domain.Adotante;
import br.com.mpet.storage.BinaryFileStore;
import br.com.mpet.storage.LZWCodec;
import br.com.mpet.storage.index.ExtensibleHashIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class AdotanteRepository implements AutoCloseable {
    private final BinaryFileStore store;
    private ExtensibleHashIndex index; // CPF -> address
    private final java.nio.file.Path indexPath;

    public AdotanteRepository(Path dataDir) throws IOException {
        this.store = new BinaryFileStore(dataDir.resolve("adotantes.dat"), new LZWCodec());
        this.indexPath = dataDir.resolve("adotantes.ehash");
        this.index = new ExtensibleHashIndex(this.indexPath);
        rebuildIfEmpty();
    }

    private void rebuildIfEmpty() throws IOException {
        store.forEach((pos, payload) -> {
            try {
                Adotante a = Adotante.fromBytes(payload);
                index.put(a.cpf, pos);
            } catch (IOException e) { /* skip */ }
        });
    }

    public boolean create(Adotante a) throws IOException {
        if (index.get(a.cpf).isPresent()) return false;
        long pos = store.append(a.toBytes());
        index.put(a.cpf, pos);
        return true;
    }

    public Optional<Adotante> readByCpf(String cpf) throws IOException {
        Optional<Long> p = index.get(cpf);
        if (p.isEmpty()) return Optional.empty();
        return store.readAt(p.get()).map(data -> {
            try { return Adotante.fromBytes(data); } catch (IOException e) { return null; }
        });
    }

    public List<Adotante> searchByNomeFragment(String frag) throws IOException {
        String f = frag.toLowerCase();
        List<Adotante> out = new ArrayList<>();
        store.forEach((pos, payload) -> {
            Adotante a = Adotante.fromBytes(payload);
            String n = a.nome == null ? "" : a.nome.toLowerCase();
            String s = a.sobrenome == null ? "" : a.sobrenome.toLowerCase();
            if (br.com.mpet.algorithms.KMP.contains(n, f) || br.com.mpet.algorithms.KMP.contains(s, f)) out.add(a);
        });
        return out;
    }

    public boolean update(Adotante a) throws IOException {
        Optional<Long> p = index.get(a.cpf);
        if (p.isEmpty()) return false;
        long newPos = store.updateAt(p.get(), a.toBytes());
        if (newPos != p.get()) index.put(a.cpf, newPos);
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
        // Recreate index using the same path, then rebuild entries
        this.index.close();
        this.index = new ExtensibleHashIndex(this.indexPath);
        store.forEach((pos, payload) -> {
            Adotante a = Adotante.fromBytes(payload);
            index.put(a.cpf, pos);
        });
    }
}
