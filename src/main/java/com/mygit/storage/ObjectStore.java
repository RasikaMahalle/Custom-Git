package com.mygit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.mygit.pack.PackManager;
import com.mygit.util.HashUtils;
import com.mygit.util.IOUtils;

public class ObjectStore {

    private final Path gitDir;
    private final Path objectsDir;
    private final PackManager packManager;

    public ObjectStore(Path gitDir) {
        this.gitDir = gitDir;
        this.objectsDir = gitDir.resolve("objects");
        this.packManager = new PackManager(objectsDir.resolve("pack"));
    }

    public static ObjectStore openDefault() {
        return new ObjectStore(Paths.get(".mygit"));
    }

    public String writeBlob(byte[] data) {
        String header = "blob " + data.length + "\0";
        byte[] store = concat(header.getBytes(), data);
        return writeObject(store);
    }

    private String writeObject(byte[] store) {
        byte[] shaBytes = HashUtils.sha1(store);
        String shaHex = HashUtils.toHex(shaBytes);

        byte[] compressed = IOUtils.zlibCompress(store);

        Path objPath = objectPath(shaHex);

        try {
            if (Files.exists(objPath))
                return shaHex;

            Files.createDirectories(objPath.getParent());

            writeAtomic(objPath, compressed);
            return shaHex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write object", e);
        }
    }

    public byte[] readObject(String shaHex) {
        Path objPath = objectsDir.resolve(shaHex.substring(0, 2)).resolve(shaHex.substring(2));

        try {
            if (Files.exists(objPath)) {
                byte[] compressed = Files.readAllBytes(objPath);
                return IOUtils.zlibDecompress(compressed);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read loose object", e);
        }

        byte[] packed = packManager.readFromPacks(shaHex);

        if (packed != null) {
            return packed;
        }

        throw new RuntimeException("Object not found: " + shaHex);

    }

    public ParseObject parseObject(byte[] store) {
        int i = 0;
        while (i < store.length && store[i] != 0)
            i++;

        if (i == store.length)
            throw new RuntimeException("Invalid object header (no header)");

        String header = new String(store, 0, i);
        String[] parts = header.split(" ");

        if (parts.length != 2)
            throw new RuntimeException("Invalid header: " + header);

        String type = parts[0];
        int size = Integer.parseInt(parts[1]);
        byte[] data = new byte[store.length - i - 1];
        System.arraycopy(store, i + 1, data, 0, data.length);

        if (size != data.length) {
            throw new RuntimeException("Size mismatch: header=" + size + " actual size=" + data.length);
        }

        return new ParseObject(type, data);
    }

    public String writeTree(byte[] treeStore) {
        String header = "tree " + treeStore.length + "\0";
        byte[] store = concat(header.getBytes(), treeStore);
        return writeObject(store);
    }

    /**
     *
     *
     * @param treeSha   tree SHA-1 hex (required)
     * @param parentSha parent commit SHA-1 hex or null
     * @param author    "Name <email>"
     * @param message   commit message
     */
    public String writeCommit(String treeSha, String parentSha, String author, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeSha).append("\n");
        if (parentSha != null && !parentSha.isBlank()) {
            sb.append("parent ").append(parentSha).append("\n");
        }

        long epoch = ZonedDateTime.now().toEpochSecond();
        String offset = ZonedDateTime.now().getOffset().getId().replace(":", "");
        sb.append("author ").append(author).append(" ").append(epoch).append(" ").append(offset).append("\n");
        sb.append("committer ").append(author).append(" ").append(epoch).append(" ").append(offset).append("\n");
        sb.append("\n");
        sb.append(message).append("\n");

        byte[] payload = sb.toString().getBytes();
        String header = "commit " + payload.length + "\0";
        byte[] store = concat(header.getBytes(), payload);

        return writeObject(store);
    }

    private Path objectPath(String shaHex) {
        String a = shaHex.substring(0, 2);
        String b = shaHex.substring(2);
        return objectsDir.resolve(a).resolve(b);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(a);
            out.write(b);

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to concat", e);
        }
    }

    private void writeAtomic(Path target, byte[] data) throws IOException {
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path tmp = parent.resolve(".tmp-" + UUID.randomUUID().toString());
        Files.write(tmp, data, StandardOpenOption.CREATE_NEW);

        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static class ParseObject {
        public final String type;
        public final byte[] data;

        public ParseObject(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
}
