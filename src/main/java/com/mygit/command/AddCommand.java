package com.mygit.command;

import com.mygit.storage.ObjectStore;
import com.mygit.util.FileStat;
import com.mygit.util.HashUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.mygit.ignore.IgnoreMatcher;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;

public class AddCommand {
    private final ObjectStore store;
    private final Index index;
    private final IgnoreMatcher ignore;

    public AddCommand() {
        Path repoRoot = Paths.get("").toAbsolutePath();
        this.store = ObjectStore.openDefault();
        this.index = new Index(Paths.get(".mygit"));
        this.ignore = new IgnoreMatcher(repoRoot);

        try {
            index.load();
        } catch (Exception e) {

        }
    }

    public void run(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                System.out.println("File not found: " + filePath);
                return;
            }

            String relativePath = repoRelative(path);

            // 🚫 Ignore handling
            if (ignore.isIgnored(relativePath)) {
                System.out.println("Ignored: " + relativePath);
                return;
            }

            // Read file content
            byte[] content = Files.readAllBytes(path);

            String blobSha = store.writeBlob(content);

            // Collect file stat (VERY IMPORTANT)
            FileStat stat = FileStat.fromPath(path);

            // Build index entry
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

            // ✅ Correct index update
            index.addorReplace(entry);
            index.save();

            System.out.println("Added: " + relativePath);

        } catch (Exception e) {
            throw new RuntimeException("Add failed", e);
        }
    }

    private String repoRelative(Path path) {
        Path root = Paths.get("").toAbsolutePath();
        return root
                .relativize(path.toAbsolutePath())
                .toString()
                .replace("\\", "/");
    }
}
