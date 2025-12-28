package com.mygit.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.mygit.RefsUtil;
import com.mygit.ignore.IgnoreMatcher;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;
import com.mygit.util.FileStat;
import com.mygit.util.HashUtils;

public class SwitchCommand {

    private final ObjectStore store;
    private final TreeReader reader;
    private final Index index;
    private final IgnoreMatcher ignore;

    public SwitchCommand() {
        this.store = ObjectStore.openDefault();
        this.reader = new TreeReader(store);
        this.index = new Index(Paths.get(".mygit"));
        this.ignore = new IgnoreMatcher(Paths.get("").toAbsolutePath());
    }

    public void run(String branch) {
        run(new String[] { branch });
    }

    public void run(String[] args) {

        if (args.length == 0) {
            throw new RuntimeException("Usage: mygit switch <branch> | mygit switch -create <branch>");
        }
        boolean create = false;
        String branch;

        if ("-create".equals(args[0])) {
            if (args.length != 2) {
                throw new RuntimeException("Usage: mygit switch -create <branch>");
            }
            create = true;
            branch = args[1];
        } else {
            branch = args[0];
        }

        // 1. Resolve Target
        String targetSha;
        if (create) {
            if (RefsUtil.listBranches().contains(branch)) {
                throw new RuntimeException("Branch already exists.");
            }
            // For -create, we branch from current HEAD but stay on it until switch succeeds?
            // Actually `switch -c` implies creating AND switching.
            // But we need to know the commit SHA to checkout.
            // If we are creating a new branch, it points to HEAD.
            targetSha = RefsUtil.readHEAD();
        } else {
            if (!RefsUtil.listBranches().contains(branch)) {
                throw new RuntimeException("Branch does not exist: " + branch);
            }
            // Read the tip of the target branch
            // Note: switchBranch just updates HEAD file, but we need the SHA *before* switching ref
            // Wait, RefsUtil.switchBranch(branch) updates HEAD to point to refs/heads/branch.
            // But we need to checkout the content of that branch.
            // So first, let's resolve the SHA of the target branch.
            targetSha = RefsUtil.resolveBranch(branch); 
        }

        // 2. Safety Check (Dirty State)
        StatusCommand status = new StatusCommand();
        if (status.isDirty()) {
             // In real Git, we can switch if changes don't conflict, but user constraint says:
             // "Throw an error if working tree is dirty (like real Git), unless forced"
             // Simplified: fail if dirty.
             throw new RuntimeException("Working tree is dirty. Commit or stash changes before switching.");
        }

        // 3. Load Target Tree
        ObjectStore.ParseObject commit = store.parseObject(store.readObject(targetSha));
        if (!"commit".equals(commit.type)) {
            throw new RuntimeException("Target is not a commit: " + targetSha);
        }
        String targetTreeSha = extractTreeSha(new String(commit.data));

        // 4. Perform Checkout (Working Tree + Index)
        try {
            checkoutTree(targetTreeSha);
        } catch (Exception e) {
            throw new RuntimeException("Checkout failed", e);
        }

        // 5. Update Refs (Only if checkout succeeded)
        if (create) {
            RefsUtil.createBranch(branch);
        }
        RefsUtil.switchBranch(branch);

        System.out.println("Switched to branch '" + branch + "'");
    }

    // =========================================================
    // CORE CHECKOUT LOGIC
    // =========================================================

    private void checkoutTree(String treeSha) throws IOException {
        
        // Load current state
        try {
            index.load();
        } catch (Exception e) {
            // If index is missing/corrupt, proceed with empty
        }

        // Load target tree structure
        Map<String, TreeEntry> targetFiles = new HashMap<>();
        walkTree(treeSha, "", targetFiles);

        // A. Update Working Directory
        // -----------------------------------------------------
        
        // 1. Delete tracked files not in target
        // (Iterate over Index because it represents tracked files)
        for (IndexEntry ie : index.getEntries()) {
            if (!targetFiles.containsKey(ie.path)) {
                Path path = Paths.get(ie.path);
                if (Files.exists(path)) {
                    Files.delete(path);
                    deleteEmptyParents(path);
                }
            }
        }

        // 2. Update/Create files from target
        for (Map.Entry<String, TreeEntry> entry : targetFiles.entrySet()) {
            String pathStr = entry.getKey();
            TreeEntry te = entry.getValue();
            Path path = Paths.get(pathStr);

            // Read blob content
            ObjectStore.ParseObject blob = store.parseObject(store.readObject(te.shaHex));
            if (!"blob".equals(blob.type)) continue;

            // Write file
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, blob.data);
            
            // Set executable if needed
            if ("100755".equals(te.mode)) {
                path.toFile().setExecutable(true);
            }
        }

        // B. Update Index
        // -----------------------------------------------------
        index.clear(); // Reset index
        
        for (Map.Entry<String, TreeEntry> entry : targetFiles.entrySet()) {
            String pathStr = entry.getKey();
            TreeEntry te = entry.getValue();
            Path path = Paths.get(pathStr);

            // Create fresh IndexEntry
            // We need file stats from disk for the index to be valid
            FileStat stat = FileStat.fromPath(path);
            
            IndexEntry ie = new IndexEntry();
            ie.path = pathStr;
            ie.sha1 = HashUtils.fromHex(te.shaHex);
            ie.flags = (short) pathStr.length(); // Simplified flags
            
            // Populate stat fields
            ie.ctimeSecs = (int) (stat.ctime / 1000);
            ie.ctimeNsecs = (int) (stat.ctime % 1000) * 1000000;
            ie.mtimeSecs = (int) (stat.mtime / 1000);
            ie.mtimeNsecs = (int) (stat.mtime % 1000) * 1000000;
            ie.dev = 0;
            ie.ino = 0;
            ie.mode = Integer.parseInt(te.mode, 8); // Mode from tree (octal string to int)
            ie.uid = 0;
            ie.gid = 0;
            ie.fileSize = (int) stat.size;
            
            index.addEntry(ie);
        }
        
        index.write();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void walkTree(String treeSha, String base, Map<String, TreeEntry> out) {
        for (TreeEntry e : reader.readTree(treeSha)) {
            String path = base.isEmpty() ? e.name : base + "/" + e.name;
            if (e.isTree()) {
                walkTree(e.shaHex, path, out);
            } else {
                out.put(path, e);
            }
        }
    }

    private String extractTreeSha(String commitBody) {
        for (String line : commitBody.split("\n")) {
            if (line.startsWith("tree ")) {
                return line.substring(5).trim();
            }
        }
        throw new RuntimeException("Commit has no tree");
    }

    private void deleteEmptyParents(Path path) {
        Path parent = path.getParent();
        while (parent != null && !parent.toString().isEmpty()) {
            if (Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isPresent()) {
                        break; 
                    }
                } catch (IOException e) {
                    break;
                }
                try {
                    Files.delete(parent);
                } catch (IOException e) {
                    break;
                }
                parent = parent.getParent();
            } else {
                break;
            }
        }
    }
}
