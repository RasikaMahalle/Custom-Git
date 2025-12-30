package com.mygit.command;

import com.mygit.ignore.IgnoreMatcher;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.util.FileStat;
import com.mygit.util.HashUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class AddCommand {

    private final ObjectStore store;
    private final Index index;
    private final IgnoreMatcher ignore;
    private final Path repoRoot;

    public AddCommand() {
        this.repoRoot = Paths.get("").toAbsolutePath().normalize();
        this.store = ObjectStore.openDefault();
        this.index = new Index(Paths.get(".mygit"));
        this.ignore = new IgnoreMatcher(repoRoot);

        try {
            index.load();
        } catch (Exception ignored) {
        }
    }

    public void run(String target) {
        Path path = repoRoot.resolve(target).normalize();

        if (!Files.exists(path)) {
            System.out.println("Path not found: " + target);
            return;
        }

        try {
            if (Files.isDirectory(path)) {
                addDirectory(path);
            } else {
                addFile(path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Add failed", e);
        }
    }

    // ================= CORE =================

    private void addDirectory(Path dir) throws Exception {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(repoRoot.resolve(".mygit")))
                    .forEach(p -> {
                        try {
                            addFile(p);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void addFile(Path path) throws Exception {

        String relativePath = repoRelative(path);

        // Ignore rules
        if (ignore.isIgnored(relativePath)) {
            return;
        }

        byte[] content = Files.readAllBytes(path);
        String blobSha = store.writeBlob(content);

        FileStat stat = FileStat.fromPath(path);

        IndexEntry entry = new IndexEntry();
        entry.ctimeSecs = (int) stat.ctime;
        entry.ctimeNsecs = 0;
        entry.mtimeSecs = (int) stat.mtime;
        entry.mtimeNsecs = 0;
        entry.dev = 0;
        entry.ino = 0;
        entry.mode = stat.executable ? 0100755 : 0100644;
        entry.uid = 0;
        entry.gid = 0;
        entry.fileSize = (int) stat.size;
        entry.sha1 = HashUtils.hexToBytes(blobSha);
        entry.path = relativePath;
        entry.flags = (short) Math.min(relativePath.length(), 0xFFF);

        index.addorReplace(entry);
        index.save();

        System.out.println("Added: " + relativePath);
    }

    // ================= UTILS =================

    private String repoRelative(Path path) {
        return repoRoot
                .relativize(path.toAbsolutePath())
                .toString()
                .replace("\\", "/");
    }
}
