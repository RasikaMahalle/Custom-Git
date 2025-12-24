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

        // ---------- RESTORE TREE ----------
        Set<Path> written = new HashSet<>();
        restoreTree(treeSha, Paths.get(""), written);
        cleanWorkingDirectory(written);

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

        for (String path : targetTree.keySet()) {

            // 1️⃣ Modified tracked file overwritten?
            if (modifiedTracked.contains(path)) {
                String curSha = currentTree.get(path);
                String tgtSha = targetTree.get(path);

                if (!Objects.equals(curSha, tgtSha)) {
                    throw new RuntimeException(
                            "Local changes to '" + path + "' would be overwritten by checkout");
                }
            }

            // 2️⃣ Untracked file collision?
            if (untracked.contains(path) && !currentTree.containsKey(path)) {
                throw new RuntimeException(
                        "Untracked file '" + path + "' would be overwritten by checkout");
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
    // RESTORE
    // =========================================================

    private void restoreTree(String treeSha, Path base, Set<Path> written) {
        for (TreeEntry e : reader.readTree(treeSha)) {
            Path fullPath = base.resolve(e.name).normalize();
            written.add(fullPath);

            if (e.isTree()) {
                try {
                    Files.createDirectories(fullPath);
                } catch (IOException ex) {
                    throw new RuntimeException(
                            "Failed to create directory: " + fullPath, ex);
                }
                restoreTree(e.shaHex, fullPath, written);
            } else {
                restoreBlob(e.shaHex, fullPath, e.mode);
            }
        }
    }

    private void restoreBlob(String sha, Path target, String mode) {
        ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));

        if (!"blob".equals(po.type)) {
            throw new RuntimeException("Not a blob: " + sha);
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, po.data);
            if ("100755".equals(mode)) {
                target.toFile().setExecutable(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed restoring " + target, e);
        }
    }

    private void cleanWorkingDirectory(Set<Path> keep) {
        try {
            Path root = Paths.get("").toAbsolutePath();
            Path gitDir = root.resolve(".mygit");

            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(gitDir))
                    .filter(p -> !keep.contains(root.relativize(p)))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Cleanup failed", e);
        }
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
