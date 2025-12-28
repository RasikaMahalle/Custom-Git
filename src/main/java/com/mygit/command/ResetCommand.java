package com.mygit.command;

import java.nio.file.Paths;
import java.util.Map;

import com.mygit.RefsUtil;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;
import com.mygit.util.HashUtils;

public class ResetCommand {

    private final ObjectStore store = ObjectStore.openDefault();
    private final TreeReader reader = new TreeReader(store);

    public void run(String[] args) {

        if (args.length != 1) {
            throw new RuntimeException("Usage: reset --hard | reset --mixed");
        }

        if ("--hard".equals(args[0])) {
            resetHard();
        } else if ("--mixed".equals(args[0])) {
            resetMixed();
        } else {
            throw new RuntimeException("Unknown reset mode: " + args[0]);
        }
    }

    // ---------- HARD RESET ----------

    private void resetHard() {

        String headSha = RefsUtil.readHEAD();
        if (headSha == null) {
            throw new RuntimeException("No commits yet.");
        }

        // 1️⃣ Restore working tree
        new RestoreCommand().run(".");

        // 2️⃣ Reset index
        resetIndexToCommit(headSha);

        System.out.println("HEAD reset --hard");
    }

    // ---------- MIXED RESET ----------

    private void resetMixed() {

        String headSha = RefsUtil.readHEAD();
        if (headSha == null) {
            throw new RuntimeException("No commits yet.");
        }

        // Only reset index
        resetIndexToCommit(headSha);

        System.out.println("Index reset to HEAD (mixed)");
    }

    // ---------- shared helpers ----------

    private void resetIndexToCommit(String commitSha) {

        Index index = new Index(Paths.get(".mygit"));
        index.getEntries().clear();

        Map<String, String> files = loadTreeFiles(commitSha);

        for (Map.Entry<String, String> e : files.entrySet()) {
            IndexEntry ie = new IndexEntry();
            ie.path = e.getKey();
            ie.sha1 = HashUtils.hexToBytes(e.getValue());
            ie.mode = 0100644;
            index.addorReplace(ie);
        }

        try {
            index.save();
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset index", e);
        }
    }

    private Map<String, String> loadTreeFiles(String commitSha) {
        ObjectStore.ParseObject commit = store.parseObject(store.readObject(commitSha));

        String treeSha = extractTreeSha(new String(commit.data));

        Map<String, String> out = new java.util.HashMap<>();
        loadTree(treeSha, "", out);
        return out;
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

    private String extractTreeSha(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("tree ")) {
                return line.substring(5).trim();
            }
        }
        throw new RuntimeException("Commit has no tree");
    }
}
