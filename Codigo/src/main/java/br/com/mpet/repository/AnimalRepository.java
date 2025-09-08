package br.com.mpet.repository;

import br.com.mpet.domain.Animal;
import br.com.mpet.storage.BinaryFileStore;
import br.com.mpet.storage.LZWCodec;
import br.com.mpet.storage.index.ExtensibleHashIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class AnimalRepository implements AutoCloseable {
    private final BinaryFileStore store;
    private ExtensibleHashIndex index; // id -> address
    private final java.nio.file.Path indexPath;

    public AnimalRepository(Path dataDir) throws IOException {
        this.store = new BinaryFileStore(dataDir.resolve("animais.dat"), new LZWCodec());
        this.indexPath = dataDir.resolve("animais.ehash");
        this.index = new ExtensibleHashIndex(this.indexPath);
        rebuildIfEmpty();
    }

    private void rebuildIfEmpty() throws IOException {
        store.forEach((pos, payload) -> {
            try {
                Animal a = Animal.fromBytes(payload);
                index.put(a.id, pos);
            } catch (IOException e) { /* skip */ }
        });
    }

    public boolean create(Animal a) throws IOException {
        if (index.get(a.id).isPresent()) return false;
        long pos = store.append(a.toBytes());
        index.put(a.id, pos);
        return true;
    }

    public Optional<Animal> readById(String id) throws IOException {
        Optional<Long> p = index.get(id);
        if (p.isEmpty()) return Optional.empty();
        return store.readAt(p.get()).map(data -> {
            try { return Animal.fromBytes(data); } catch (IOException e) { return null; }
        });
    }

    public List<Animal> search(String nome, String especie, Boolean castrado) throws IOException {
        String fn = nome == null ? null : nome.toLowerCase();
        String fe = especie == null ? null : especie.toLowerCase();
        List<Animal> out = new ArrayList<>();
        store.forEach((pos, payload) -> {
            Animal a = Animal.fromBytes(payload);
            boolean ok = true;
            if (fn != null) ok &= br.com.mpet.algorithms.KMP.contains(a.nome == null ? "" : a.nome.toLowerCase(), fn);
            if (fe != null) ok &= br.com.mpet.algorithms.KMP.contains(a.especie == null ? "" : a.especie.toLowerCase(), fe);
            if (castrado != null) ok &= a.seCastrado == castrado;
            if (ok) out.add(a);
        });
        return out;
    }

    public boolean update(Animal a) throws IOException {
        Optional<Long> p = index.get(a.id);
        if (p.isEmpty()) return false;
        long newPos = store.updateAt(p.get(), a.toBytes());
        if (newPos != p.get()) index.put(a.id, newPos);
        return true;
    }

    public boolean delete(String id) throws IOException {
        Optional<Long> p = index.get(id);
        if (p.isEmpty()) return false;
        boolean ok = store.deleteAt(p.get());
        if (ok) index.remove(id);
        return ok;
    }

    @Override public void close() throws IOException { index.close(); store.close(); }

    public void vacuum() throws IOException {
        store.vacuum();
        // Recreate index using the same path and rebuild
        this.index.close();
        this.index = new ExtensibleHashIndex(this.indexPath);
        store.forEach((pos, payload) -> {
            Animal a = Animal.fromBytes(payload);
            index.put(a.id, pos);
        });
    }
}
