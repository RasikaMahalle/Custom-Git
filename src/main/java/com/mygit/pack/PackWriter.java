package com.mygit.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.mygit.storage.ObjectStore;

import com.mygit.util.HashUtils;
import com.mygit.util.IOUtils;
import com.mygit.util.ThreadPool;

import com.mygit.storage.ObjectStore.ParseObject;

public class PackWriter {
    private final ObjectStore store;
    private final Path packDir;

    public PackWriter() {
        this.store = ObjectStore.openDefault();
        this.packDir = Paths.get(".mygit").resolve("objects").resolve("pack");
    }

    public PackResult packObjects(Set<String> shas) {
        try {
            Files.createDirectories(packDir);

            long ts = System.currentTimeMillis() / 1000;
            Path packPath = packDir.resolve("pack-" + ts + ".pack");
            Path idxPath = packDir.resolve("pack-" + ts + ".idx");

            Map<String, byte[]> compressedMap = compressAll(shas);

            Map<String, Long> offsets = new LinkedHashMap<>();
            try (OutputStream out = Files.newOutputStream(packPath, java.nio.file.StandardOpenOption.CREATE_NEW)) {
                out.write("PACK".getBytes());
                writeInt(out, 1);
                writeInt(out, compressedMap.size());

                long offset = 4 + 4 + 4;

                for (Map.Entry<String, byte[]> e : compressedMap.entrySet()) {
                    String sha = e.getKey();
                    byte[] comp = e.getValue();
                    offsets.put(sha, offset);

                    out.write(HashUtils.hexToBytes(sha));
                    offset += 20;

                    writeInt(out, comp.length);
                    offset += 4;

                    out.write(comp);
                    offset += comp.length;
                }
            }
            try (OutputStream idxOut = Files.newOutputStream(idxPath, java.nio.file.StandardOpenOption.CREATE_NEW)) {
                idxOut.write("IDX1".getBytes());
                writeInt(idxOut, offsets.size());
                for (Map.Entry<String, Long> e : offsets.entrySet()) {
                    idxOut.write(HashUtils.hexToBytes(e.getKey()));
                    writeLong(idxOut, e.getValue());
                }
            }
            return new PackResult(packPath, idxPath);
        } catch (Exception e) {
            throw new RuntimeException("Pack creation failed", e);
        }
    }

    private Map<String, byte[]> compressAll(Set<String> shas)
            throws InterruptedException, ExecutionException {

        ExecutorService pool = ThreadPool.get();

        // Sort SHAs to get deterministic base selection
        List<String> sorted = new ArrayList<>(shas);
        Collections.sort(sorted);

        List<Callable<Map.Entry<String, byte[]>>> tasks = new ArrayList<>();

        // Keep previous blob as delta base candidate
        final Map<String, byte[]> blobCache = new HashMap<>();
        final Map<String, byte[]> fullObjectCache = new HashMap<>();

        for (String sha : sorted) {
            tasks.add(() -> {
                byte[] storeBytes = store.readObject(sha);
                ObjectStore.ParseObject po = store.parseObject(storeBytes);

                // Reconstruct full object format: "<type> <size>\0<data>"
                String header = po.type + " " + po.data.length + "\0";
                byte[] fullObject = concat(header.getBytes("UTF-8"), po.data);

                // Default: full object compression
                byte[] bestCompressed = IOUtils.zlibCompress(fullObject);

                // Try delta compression ONLY for blobs
                if ("blob".equals(po.type)) {

                    for (Map.Entry<String, byte[]> baseEntry : blobCache.entrySet()) {
                        String baseSha = baseEntry.getKey();
                        byte[] baseData = baseEntry.getValue();

                        // Generate delta instructions
                        List<com.mygit.pack.delta.DeltaInstruction> ops = com.mygit.pack.delta.DeltaEncoder
                                .encode(baseData, po.data);

                        byte[] deltaPayload = com.mygit.pack.delta.DeltaSerializer.serialize(baseSha, ops);

                        // Delta object format: "delta\n<delta-data>"
                        byte[] deltaObject = concat(
                                "delta\n".getBytes("UTF-8"),
                                deltaPayload);

                        byte[] compressedDelta = IOUtils.zlibCompress(deltaObject);

                        // Choose delta only if smaller
                        if (compressedDelta.length < bestCompressed.length) {
                            bestCompressed = compressedDelta;
                        }
                    }

                    // Cache this blob for future delta bases
                    blobCache.put(sha, po.data);
                }

                return Map.entry(sha, bestCompressed);
            });
        }

        List<Future<Map.Entry<String, byte[]>>> futures = pool.invokeAll(tasks);

        Map<String, byte[]> result = new LinkedHashMap<>();
        for (Future<Map.Entry<String, byte[]>> f : futures) {
            Map.Entry<String, byte[]> e = f.get();
            result.put(e.getKey(), e.getValue());
        }

        return result;
    }

    private static byte[] concat(byte[] a, byte[] b) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(a);
            out.write(b);
            return out.toByteArray();
        }
    }

    private static void writeInt(OutputStream out, int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v) & 0xFF);
    }

    private static void writeLong(OutputStream out, long v) throws IOException {
        out.write((int) ((v >>> 56) & 0xFF));
        out.write((int) ((v >>> 48) & 0xFF));
        out.write((int) ((v >>> 40) & 0xFF));
        out.write((int) ((v >>> 32) & 0xFF));
        out.write((int) ((v >>> 24) & 0xFF));
        out.write((int) ((v >>> 16) & 0xFF));
        out.write((int) ((v >>> 8) & 0xFF));
        out.write((int) ((v) & 0xFF));
    }

    public static class PackResult {
        public final Path pack;
        public final Path idx;

        public PackResult(Path pack, Path idx) {
            this.pack = pack;
            this.idx = idx;
        }
    }
}
