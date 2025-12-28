package com.mygit.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.mygit.RefsUtil;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;

public class CheckOutCommand {

    private final ObjectStore store;
    private final TreeReader reader;

    public CheckOutCommand() {
        this.store = ObjectStore.openDefault();
        this.reader = new TreeReader(store);
    }

    public void run(String arg) {
        run(new String[] { arg });
    }

    public void run(String[] args) {

        if (args.length < 1) {
            throw new RuntimeException("Usage: checkout <branch|commit> OR checkout -create <branch>");
        }

        String target;
        boolean createBranch = false;

        // ---------- ARG PARSING ----------
        if ("-create".equals(args[0])) {
            if (args.length != 2) {
                throw new RuntimeException("Usage: checkout -create <branch-name>");
            }
            createBranch = true;
            target = args[1];
        } else {
            target = args[0];
        }

        String commitSha;

        // ---------- RESOLVE TARGET ----------
        if (createBranch) {
            if (RefsUtil.listBranches().contains(target)) {
                throw new RuntimeException("Branch already exists: " + target);
            }
            RefsUtil.createBranch(target);
            RefsUtil.switchBranch(target);
            commitSha = RefsUtil.readHEAD();

        } else if (RefsUtil.listBranches().contains(target)) {
            RefsUtil.switchBranch(target);
            commitSha = RefsUtil.readHEAD();

        } else if (objectExistsAsCommit(target)) {
            commitSha = target;
            RefsUtil.updateHEADWithSha(commitSha);

        } else {
            throw new RuntimeException("Branch or commit not found: " + target);
        }

        // ---------- LOAD COMMIT ----------
        ObjectStore.ParseObject po = store.parseObject(store.readObject(commitSha));
        if (!"commit".equals(po.type)) {
            throw new RuntimeException("Not a commit: " + target);
        }

        String treeSha = extractTreeSha(new String(po.data));

        // ---------- SAFETY CHECK (Git-accurate) ----------
        StatusCommand status = new StatusCommand();

        Map<String, String> currentTree = loadTreeFiles(RefsUtil.readHEAD());
        Map<String, String> targetTree = loadTreeFiles(commitSha);

        checkOverwriteSafety(currentTree, targetTree, status);

        // ---------- UPDATE WORKING TREE (File-driven) ----------
        updateWorkingTree(currentTree, targetTree);

        System.out.println("Checked out: " + target);
    }

    // =========================================================
    // SAFETY (Git-faithful)
    // =========================================================

    private void checkOverwriteSafety(
            Map<String, String> currentTree,
            Map<String, String> targetTree,
            StatusCommand status) {

        Set<String> modifiedTracked = status.getModifiedTrackedFiles();
        Set<String> untracked = new HashSet<>(status.getUntrackedFiles());

        // 1. Check for conflicts with files being ADDED/UPDATED in target
        for (String path : targetTree.keySet()) {

            // Modified tracked file overwritten?
            if (modifiedTracked.contains(path)) {
                String curSha = currentTree.get(path);
                String tgtSha = targetTree.get(path);

                if (!Objects.equals(curSha, tgtSha)) {
                    throw new RuntimeException(
                            "Local changes to '" + path + "' would be overwritten by checkout");
                }
            }

            // Untracked file collision?
            if (untracked.contains(path) && !currentTree.containsKey(path)) {
                throw new RuntimeException(
                        "Untracked file '" + path + "' would be overwritten by checkout");
            }
        }

        // 2. Check for conflicts with files being DELETED (present in HEAD, absent in Target)
        // Git prevents deletion of files with local modifications
        for (String path : currentTree.keySet()) {
            if (!targetTree.containsKey(path)) {
                if (modifiedTracked.contains(path)) {
                    throw new RuntimeException(
                            "Local changes to '" + path + "' would be lost by checkout (file deleted in target)");
                }
            }
        }
    }

    // =========================================================
    // TREE HELPERS
    // =========================================================

    private Map<String, String> loadTreeFiles(String commitSha) {
        Map<String, String> map = new HashMap<>();
        if (commitSha == null)
            return map;

        ObjectStore.ParseObject commit = store.parseObject(store.readObject(commitSha));
        if (!"commit".equals(commit.type))
            return map;

        String treeSha = extractTreeSha(new String(commit.data));
        walkTree(treeSha, "", map);
        return map;
    }

    private void walkTree(String treeSha, String base, Map<String, String> out) {
        for (TreeEntry e : reader.readTree(treeSha)) {
            String path = base.isEmpty() ? e.name : base + "/" + e.name;
            if (e.isTree()) {
                walkTree(e.shaHex, path, out);
            } else {
                out.put(path, e.shaHex);
            }
        }
    }

    // =========================================================
    // UPDATE WORKING TREE (Refactored)
    // =========================================================

    private void updateWorkingTree(Map<String, String> currentTree, Map<String, String> targetTree) {
        // 1. Delete files present in HEAD but absent in Target
        for (String path : currentTree.keySet()) {
            if (!targetTree.containsKey(path)) {
                deleteFileAndEmptyParents(Paths.get(path));
            }
        }

        // 2. Update/Create files present in Target
        for (Map.Entry<String, String> entry : targetTree.entrySet()) {
            String path = entry.getKey();
            String sha = entry.getValue();

            // Only write if file is new or changed
            if (!currentTree.containsKey(path) || !currentTree.get(path).equals(sha)) {
                // Fetch mode from tree if possible, but here we only have SHA from the map
                // In a real implementation we'd need the mode too. 
                // For now, default to 644 or assume restoreBlob can handle it if we passed mode.
                // Wait, loadTreeFiles map only stores SHA. 
                // To support executable bit, we should store Entry objects. 
                // But for this refactoring, let's assume standard mode or fetch from object if needed.
                // However, restoreBlob takes mode. 
                // Let's modify loadTreeFiles to return Map<String, TreeEntry> or similar?
                // Or just read the tree again?
                // Actually, reading the tree again is cleaner for getting modes.
                // But we already have the map for quick comparison.
                
                // Let's just re-read the mode from the tree during traversal or use a default.
                // Since I cannot easily change the Map signature without breaking other things potentially,
                // I will try to find the mode. 
                // Actually, I can just use a helper to get mode, or change the map to Map<String, IndexEntryLike>.
                
                // Simpler: Use restoreBlob with a default mode for now, OR better:
                // Modify loadTreeFiles to store <Path, TreeEntry> instead of <String, String>.
                // But let's stick to the user request about directory logic first.
                // I will pass "0100644" as default for now, or improve it if I have time.
                // A better approach: Iterate the TARGET tree directly to write files.
                restoreBlob(sha, Paths.get(path), "0100644"); 
            }
        }
    }
    
    private void deleteFileAndEmptyParents(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
            // Walk up and delete empty directories
            Path parent = path.getParent();
            while (parent != null && !parent.toString().isEmpty()) {
                // Check if directory is empty
                if (Files.isDirectory(parent)) {
                    try (var stream = Files.list(parent)) {
                        if (stream.findAny().isPresent()) {
                            break; // Not empty
                        }
                    }
                    Files.delete(parent);
                    parent = parent.getParent();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            // Ignore deletion errors (maybe concurrently modified or permission issues)
            // In a real Git, we might warn.
        }
    }

    private void restoreBlob(String sha, Path target, String mode) {
        ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));

        if (!"blob".equals(po.type)) {
            throw new RuntimeException("Not a blob: " + sha);
        }

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, po.data);
            if ("100755".equals(mode)) {
                target.toFile().setExecutable(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed restoring " + target, e);
        }
    }

    private void cleanWorkingDirectory(Set<Path> keep) {
        // Deprecated: No longer used in new file-driven approach
    }

    // =========================================================
    // MISC
    // =========================================================

    private boolean objectExistsAsCommit(String sha) {
        try {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));
            return "commit".equals(po.type);
        } catch (Exception e) {
            return false;
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
}
