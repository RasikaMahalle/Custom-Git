package com.mygit.pack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.mygit.util.HashUtils;

public class PackIndex {
    private final Map<String, Long> offsetBySha = new HashMap<>();

    public PackIndex(Path idxPath) {
        load(idxPath);
    }

    private void load(Path idxPath) {
        try (InputStream in = Files.newInputStream(idxPath)) {
            byte[] hdr = in.readNBytes(4);
            if (!new String(hdr).equals("IDX1")) {
                throw new RuntimeException("Invalid pack index");
            }

            int count = readInt(in);
            for (int i = 0; i < count; i++) {
                byte[] shaBytes = in.readNBytes(20);
                long offset = readLong(in);
                offsetBySha.put(HashUtils.toHex(shaBytes), offset);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load pack index", e);
        }
    }

    private int readInt(InputStream in) throws Exception {
        return (in.read() << 24)
                | (in.read() << 16)
                | (in.read() << 8)
                | in.read();
    }

    private long readLong(InputStream in) throws Exception {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (in.read() & 0xFF);
        }
        return v;
    }

    public boolean contains(String sha) {
        return offsetBySha.containsKey(sha);
    }

    public long offsetOf(String sha) {
        return offsetBySha.get(sha);
    }
}
