package com.mygit.command;

import java.nio.file.Paths;
import java.util.Map;

import com.mygit.RefsUtil;
import com.mygit.index.Index;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;

public class RestoreCommand {

    private final ObjectStore store = ObjectStore.openDefault();
    private final TreeReader reader = new TreeReader(store);

    public void run(String arg) {
        run(new String[] { arg });
    }

    public void run(String[] args) {

        if (args.length == 0) {
            throw new RuntimeException("Usage: restore [--staged] <file|.>");
        }

        if ("--staged".equals(args[0])) {
            if (args.length != 2) {
                throw new RuntimeException("Usage: restore --staged <file|.>");
            }
            restoreStaged(args[1]);
        } else {
            restoreWorkingTree(args[0]);
        }
    }

    // ---------- restore working tree (HEAD → WD) ----------

    private void restoreWorkingTree(String target) {

        String headSha = RefsUtil.readHEAD();
        if (headSha == null) {
            throw new RuntimeException("No commits yet.");
        }

        Map<String, String> files = loadTreeFiles(headSha);

        if (".".equals(target)) {
            for (Map.Entry<String, String> e : files.entrySet()) {
                restoreFile(e.getKey(), e.getValue());
            }
            System.out.println("Restored all files.");
            return;
        }

        if (!files.containsKey(target)) {
            throw new RuntimeException("File not found in HEAD: " + target);
        }

        restoreFile(target, files.get(target));
        System.out.println("Restored: " + target);
    }

    // ---------- restore staged (index only) ----------

    private void restoreStaged(String target) {

        Index index = new Index(Paths.get(".mygit"));
        try {
            index.load();
        } catch (Exception e) {
        }

        if (".".equals(target)) {
            index.getEntries().clear();
            System.out.println("Unstaged all files.");
        } else {
            index.remove(target);
            System.out.println("Unstaged: " + target);
        }

        try {
            index.save();
        } catch (Exception e) {
        }
    }

    // ---------- helpers ----------

    private Map<String, String> loadTreeFiles(String commitSha) {
        ObjectStore.ParseObject commit = store.parseObject(store.readObject(commitSha));
        String treeSha = extractTreeSha(new String(commit.data));

        Map<String, String> files = new java.util.HashMap<>();
        loadTree(treeSha, "", files);
        return files;
    }

    private void loadTree(String treeSha, String base, Map<String, String> out) {
        for (TreeEntry e : reader.readTree(treeSha)) {
            String path = base.isEmpty() ? e.name : base + "/" + e.name;
            if (e.isTree()) {
                loadTree(e.shaHex, path, out);
            } else {
                out.put(path, e.shaHex);
            }
        }
    }

    private void restoreFile(String path, String sha) {
        try {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            java.nio.file.Path parent = p.getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
            java.nio.file.Files.write(p, po.data);
        } catch (Exception e) {
            throw new RuntimeException("Failed restoring " + path, e);
        }
    }

    private String extractTreeSha(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("tree ")) {
                return line.substring(5).trim();
            }
        }
        throw new RuntimeException("Commit has no tree");
    }
}
