package br.com.mpet.storage.index;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Extensible Hashing (directory + buckets) for String->Long. In-memory with persistence on close and load on init.
 */
public class ExtensibleHashIndex implements Index<String> {
    private static final int BUCKET_SIZE = 8;

    private static class Bucket { int localDepth; Map<String, Long> entries = new HashMap<>(); }

    private final Path path;
    private int globalDepth = 1;
    private List<Bucket> directory = new ArrayList<>();

    public ExtensibleHashIndex(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            load();
        } else {
            directory.add(new Bucket());
            directory.add(new Bucket());
            directory.get(0).localDepth = directory.get(1).localDepth = 1;
        }
    }

    private int hash(String key) { return key.hashCode(); }
    private int dirIndex(String key) { return (hash(key) >>> 1) & ((1 << globalDepth) - 1); }

    @Override
    public Optional<Long> get(String key) {
        Bucket b = directory.get(dirIndex(key));
        return Optional.ofNullable(b.entries.get(key));
    }

    @Override
    public void put(String key, long address) {
        while (true) {
            Bucket b = directory.get(dirIndex(key));
            if (b.entries.size() < BUCKET_SIZE || b.entries.containsKey(key)) {
                b.entries.put(key, address);
                return;
            }
            splitBucket(dirIndex(key));
        }
    }

    @Override
    public void remove(String key) {
        Bucket b = directory.get(dirIndex(key));
        b.entries.remove(key);
    }

    private void splitBucket(int idx) {
        Bucket b = directory.get(idx);
        int newLocal = b.localDepth + 1;
        if (newLocal > globalDepth) {
            // double directory
            List<Bucket> newDir = new ArrayList<>(directory);
            newDir.addAll(directory);
            directory = newDir;
            globalDepth++;
        }
        Bucket b1 = new Bucket(); b1.localDepth = newLocal;
        Bucket b2 = new Bucket(); b2.localDepth = newLocal;
        int mask = (1 << newLocal) - 1;
        for (Map.Entry<String, Long> e : b.entries.entrySet()) {
            int which = (hash(e.getKey()) & mask) >>> 0 & 1;
            if (which == 0) b1.entries.put(e.getKey(), e.getValue()); else b2.entries.put(e.getKey(), e.getValue());
        }
        // update directory pointers for all slots referencing old bucket
        for (int i = 0; i < directory.size(); i++) {
            int key = i & ((1 << (newLocal-1)) - 1);
            int base = i & ~((1 << (newLocal)) - 1);
            int i0 = base | key;
            int i1 = i0 | (1 << (newLocal-1));
            if (directory.get(i0) == b) directory.set(i0, b1);
            if (directory.get(i1) == b) directory.set(i1, b2);
        }
    }

    @Override
    public void close() throws IOException { save(); }

    private void save() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write("depth\t" + globalDepth + "\n");
            // flatten unique buckets
            Set<Bucket> uniq = new HashSet<>(directory);
            Map<Bucket, Integer> ids = new HashMap<>();
            int id = 0;
            for (Bucket b : uniq) ids.put(b, id++);
            // directory
            bw.write("dir\t" + directory.size() + "\n");
            for (Bucket b : directory) bw.write(ids.get(b) + "\n");
            // buckets
            bw.write("buckets\t" + uniq.size() + "\n");
            for (Bucket b : uniq) {
                bw.write("b\t" + b.localDepth + "\n");
                for (Map.Entry<String, Long> e : b.entries.entrySet()) {
                    bw.write("e\t" + e.getKey() + "\t" + e.getValue() + "\n");
                }
                bw.write("endb\n");
            }
        }
    }

    private void load() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            if (line == null || !line.startsWith("depth\t")) throw new IOException("Invalid index file");
            globalDepth = Integer.parseInt(line.split("\t")[1]);
            line = br.readLine();
            if (line == null || !line.startsWith("dir\t")) throw new IOException("Invalid index file");
            int dirSize = Integer.parseInt(line.split("\t")[1]);
            List<Integer> dirIds = new ArrayList<>();
            for (int i = 0; i < dirSize; i++) dirIds.add(Integer.parseInt(br.readLine()));
            line = br.readLine();
            if (line == null || !line.startsWith("buckets\t")) throw new IOException("Invalid index file");
            int bucketCount = Integer.parseInt(line.split("\t")[1]);
            List<Bucket> buckets = new ArrayList<>(Collections.nCopies(bucketCount, null));
            for (int bi = 0; bi < bucketCount; bi++) {
                line = br.readLine();
                if (line == null || !line.startsWith("b\t")) throw new IOException("Invalid index file");
                Bucket b = new Bucket();
                b.localDepth = Integer.parseInt(line.split("\t")[1]);
                while ((line = br.readLine()) != null && !line.equals("endb")) {
                    String[] parts = line.split("\t");
                    if (parts.length == 3 && parts[0].equals("e")) b.entries.put(parts[1], Long.parseLong(parts[2]));
                }
                buckets.set(bi, b);
            }
            directory = new ArrayList<>();
            for (int id : dirIds) directory.add(buckets.get(id));
        }
    }
}
