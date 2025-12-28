package com.mygit.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mygit.index.IndexEntry;
import com.mygit.util.HashUtils;

public class TreeWriter {

    static class Node {
        final Map<String, Node> dirs = new TreeMap<>();

        final List<FileEntry> files = new ArrayList<>();
    }

    static class FileEntry {
        final String name;
        final String mode;
        final String shaHex;

        FileEntry(String name, String mode, String shaHex) {
            this.name = name;
            this.mode = mode;
            this.shaHex = shaHex;
        }
    }

    private final ObjectStore store;

    public TreeWriter(ObjectStore store) {
        this.store = store;
    }

    public String writeTreeFromIndex(List<IndexEntry> indexEntries) {
        Node root = new Node();

        for (IndexEntry ie : indexEntries) {
            String path = ie.path.replace("\\", "/");
            String[] parts = path.split("/");
            Node cur = root;
            for (int i = 0; i < parts.length - 1; i++) {
                String dir = parts[i];
                cur = cur.dirs.computeIfAbsent(dir, k -> new Node());
            }
            String filename = parts[parts.length - 1];

            String mode = (Integer.toOctalString(ie.mode).equals("100755") || (ie.mode & 0b1) == 1) ? "100755"
                    : "100644";
            String blobSha = HashUtils.toHex(ie.sha1);

            cur.files.add(new FileEntry(filename, mode, blobSha));
        }
        return writeNode(root);
    }

    private String writeNode(Node node) {
        TreeBuilder tb = new TreeBuilder();

        // 1️⃣ write subtrees FIRST
        for (Map.Entry<String, Node> e : node.dirs.entrySet()) {
            String dirname = e.getKey();
            Node child = e.getValue();
            String childSha = writeNode(child);
            tb.add("40000", dirname, childSha);
        }

        // 2️⃣ then write files
        node.files.sort(Comparator.comparing(f -> f.name));
        for (FileEntry f : node.files) {
            tb.add(f.mode, f.name, f.shaHex);
        }

        byte[] raw = tb.build();
        return store.writeTree(raw);
    }

}
