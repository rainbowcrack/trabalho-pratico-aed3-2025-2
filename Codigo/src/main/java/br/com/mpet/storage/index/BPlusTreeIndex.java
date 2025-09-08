package br.com.mpet.storage.index;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * In-memory B+ Tree for String->Long with ordered iteration. Entries are persisted on close and reloaded on startup.
 */
public class BPlusTreeIndex implements Index<String> {
    private static final int BRANCHING = 32; // order

    private abstract static class Node { List<String> keys = new ArrayList<>(); }
    private static class InternalNode extends Node { List<Node> children = new ArrayList<>(); }
    private static class LeafNode extends Node { List<Long> values = new ArrayList<>(); LeafNode next; }

    private final Path path;
    private Node root = new LeafNode();

    public BPlusTreeIndex(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] parts = line.split("\t", 2);
                    if (parts.length == 2) put(parts[0], Long.parseLong(parts[1]));
                }
            }
        }
    }

    @Override
    public Optional<Long> get(String key) {
        LeafNode leaf = findLeaf(root, key);
        int i = Collections.binarySearch(leaf.keys, key);
        if (i >= 0) return Optional.of(leaf.values.get(i));
        return Optional.empty();
    }

    @Override
    public void put(String key, long address) {
        SplitResult split = insert(root, key, address);
        if (split != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(split.pivot);
            newRoot.children.add(split.left);
            newRoot.children.add(split.right);
            root = newRoot;
        }
    }

    @Override
    public void remove(String key) {
        delete(root, key);
        // no rebalancing for simplicity; tree may become shallow over time.
        if (root instanceof InternalNode in && in.children.size() == 1) root = in.children.get(0);
    }

    public Iterable<Map.Entry<String, Long>> entries() {
        List<Map.Entry<String, Long>> list = new ArrayList<>();
        LeafNode leaf = leftmostLeaf(root);
        while (leaf != null) {
            for (int i = 0; i < leaf.keys.size(); i++) list.add(Map.entry(leaf.keys.get(i), leaf.values.get(i)));
            leaf = leaf.next;
        }
        return list;
    }

    private static LeafNode findLeaf(Node node, String key) {
        if (node instanceof LeafNode l) return l;
        InternalNode in = (InternalNode) node;
        int idx = upperBound(in.keys, key);
        return findLeaf(in.children.get(idx), key);
    }

    private static int upperBound(List<String> keys, String key) {
        int lo = 0, hi = keys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (key.compareTo(keys.get(mid)) > 0) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    private record SplitResult(String pivot, Node left, Node right) {}

    private SplitResult insert(Node node, String key, long value) {
        if (node instanceof LeafNode leaf) {
            int i = Collections.binarySearch(leaf.keys, key);
            if (i >= 0) {
                leaf.values.set(i, value); // overwrite
                return null;
            }
            int pos = -(i + 1);
            leaf.keys.add(pos, key);
            leaf.values.add(pos, value);
            if (leaf.keys.size() < BRANCHING) return null;
            // split leaf
            int mid = leaf.keys.size() / 2;
            LeafNode right = new LeafNode();
            right.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
            right.values.addAll(leaf.values.subList(mid, leaf.values.size()));
            // shrink left
            leaf.keys.subList(mid, leaf.keys.size()).clear();
            leaf.values.subList(mid, leaf.values.size()).clear();
            // link
            right.next = leaf.next;
            leaf.next = right;
            String pivot = right.keys.get(0);
            return new SplitResult(pivot, leaf, right);
        } else {
            InternalNode in = (InternalNode) node;
            int idx = upperBound(in.keys, key);
            SplitResult childSplit = insert(in.children.get(idx), key, value);
            if (childSplit == null) return null;
            in.keys.add(idx, childSplit.pivot);
            in.children.set(idx, childSplit.left);
            in.children.add(idx + 1, childSplit.right);
            if (in.keys.size() < BRANCHING) return null;
            // split internal
            int mid = in.keys.size() / 2;
            String pivot = in.keys.get(mid);
            InternalNode right = new InternalNode();
            right.keys.addAll(in.keys.subList(mid + 1, in.keys.size()));
            right.children.addAll(in.children.subList(mid + 1, in.children.size()));
            // shrink left
            in.keys.subList(mid, in.keys.size()).clear();
            in.children.subList(mid + 1, in.children.size()).clear();
            return new SplitResult(pivot, in, right);
        }
    }

    private boolean delete(Node node, String key) {
        if (node instanceof LeafNode leaf) {
            int i = Collections.binarySearch(leaf.keys, key);
            if (i < 0) return false;
            leaf.keys.remove(i); leaf.values.remove(i);
            return true;
        } else {
            InternalNode in = (InternalNode) node;
            int idx = upperBound(in.keys, key);
            boolean removed = delete(in.children.get(idx), key);
            // optional: clean empty separators
            if (idx < in.keys.size()) {
                LeafNode leftLeaf = rightmostLeaf(in.children.get(idx));
                if (leftLeaf.keys.isEmpty()) { in.keys.remove(idx); in.children.remove(idx); }
            }
            return removed;
        }
    }

    private static LeafNode leftmostLeaf(Node node) {
        while (node instanceof InternalNode in) node = in.children.get(0);
        return (LeafNode) node;
    }
    private static LeafNode rightmostLeaf(Node node) {
        while (node instanceof InternalNode in) node = in.children.get(in.children.size()-1);
        return (LeafNode) node;
    }

    @Override
    public void close() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Long> e : entries()) {
                bw.write(e.getKey());
                bw.write('\t');
                bw.write(Long.toString(e.getValue()));
                bw.write('\n');
            }
        }
    }
}
