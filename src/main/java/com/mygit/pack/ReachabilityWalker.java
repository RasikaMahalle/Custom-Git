package com.mygit.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mygit.RefsUtil;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.ObjectStore.ParseObject;
import com.mygit.storage.TreeReader.TreeEntry;

public class ReachabilityWalker {
    private final ObjectStore store;
    private final TreeReader tr;

    public ReachabilityWalker() {
        this.store = ObjectStore.openDefault();
        this.tr = new TreeReader(store);
    }

    public Set<String> reachableFromRefs() {
        Set<String> result = new HashSet<>();

        try {
            Path refsHeads = Paths.get(".mygit").resolve("refs").resolve("heads");
            if (!Files.exists(refsHeads))
                return result;

            Files.walk(refsHeads, 2).filter(F -> !Files.isDirectory(F)).forEach(F -> {
                try {
                    String sha = Files.readString(F).trim();
                    collectFromCommit(sha, result);
                } catch (Exception ignored) {
                }
            });
            String headRaw = RefsUtil.readHEADRaw();
            if (headRaw != null && headRaw.startsWith("ref: ")) {
                collectFromCommit(headRaw, result);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed reading refs", e);
        }
        return result;
    }

    public boolean isReachable(String fromCommit, String targetCommit) {
        Set<String> visited = new HashSet<>();
        return dfs(fromCommit, targetCommit, visited);
    }

    private boolean dfs(String current, String target, Set<String> visited) {
        if (current == null || visited.contains(current)) {
            return false;
        }
        if (current.equals(target)) {
            return true;
        }
        visited.add(current);

        ObjectStore.ParseObject po = store.parseObject(store.readObject(current));

        String body = new String(po.data);
        for (String line : body.split("\n")) {
            if (line.startsWith("parent ")) {
                String p = line.substring(7).trim();
                if (dfs(p, target, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void collectFromCommit(String commitSha, Set<String> acc) {
        if (commitSha == null || commitSha.isBlank() || acc.contains(commitSha))
            return;
        acc.add(commitSha);

        try {
            byte[] commitStore = store.readObject(commitSha);
            ParseObject po = store.parseObject(commitStore);
            if (!"commit".equals(po.type))
                return;
            String body = new String(po.data);

            String tree = null;
            for (String line : body.split("\n")) {
                if (line.startsWith("tree ")) {
                    tree = line.substring(5).trim();
                }
                if (line.startsWith("parent ")) {
                    String p = line.substring(7).trim();
                    collectFromCommit(p, acc);
                }
            }
            if (tree != null)
                collectFromTree(tree, acc);
        } catch (Exception e) {
        }
    }

    private void collectFromTree(String treeSha, Set<String> acc) {
        if (treeSha == null || treeSha.isBlank() || acc.contains(treeSha))
            return;

        acc.add(treeSha);
        List<TreeEntry> entries = tr.readTree(treeSha);
        for (TreeEntry e : entries) {
            if (e.isTree()) {
                collectFromTree(e.shaHex, acc);
            } else {
                acc.add(e.shaHex);
            }
        }
    }

}
