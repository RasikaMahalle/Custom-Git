package com.mygit.storage;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.mygit.util.HashUtils;

public class TreeBuilder {
    public static class Entry {
        public final String mode;
        public final String name;
        public final String shaHex;

        public Entry(String mode, String name, String shaHex) {
            this.mode = mode;
            this.name = name;
            this.shaHex = shaHex;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public TreeBuilder add(String mode, String name, String shaHex) {
        entries.add(new Entry(mode, name, shaHex));
        return this;
    }

    public byte[] build() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Entry e : entries) {
                String header = e.mode + " " + e.name;
                out.write(header.getBytes());
                out.write(0);
                out.write(HashUtils.hexToBytes(e.shaHex));
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tree", e);
        }
    }
}
