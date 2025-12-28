package com.mygit.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mygit.util.HashUtils;

public class TreeReader {
    public static class TreeEntry {
        public final String mode;
        public final String name;
        public final String shaHex;

        public TreeEntry(String mode, String name, String shaHex) {
            this.mode = mode;
            this.name = name;
            this.shaHex = shaHex;
        }

        public boolean isTree() {
            return mode.equals("40000");
        }
    }

    private final ObjectStore store;

    public TreeReader(ObjectStore store) {
        this.store = store;
    }

    public List<TreeEntry> readTree(String treeSha) {
        byte[] storeBytes = store.readObject(treeSha);
        ObjectStore.ParseObject po = store.parseObject(storeBytes);

        if (!"tree".equals(po.type)) {
            throw new RuntimeException("Object is not a tree: " + treeSha);
        }

        byte[] data = po.data;
        List<TreeEntry> entries = new ArrayList<>();
        int i = 0;

        while (i < data.length) {
            int start = i;
            while (data[i] != ' ')
                i++;
            String mode = new String(data, start, i - start);
            i++;

            start = i;
            while (data[i] != 0)
                i++;
            String name = new String(data, start, i - start);
            i++;

            byte[] shaBytes = Arrays.copyOfRange(data, i, i + 20);
            i += 20;
            String shaHex = HashUtils.toHex(shaBytes);

            entries.add(new TreeEntry(mode, name, shaHex));
        }
        return entries;
    }
}
