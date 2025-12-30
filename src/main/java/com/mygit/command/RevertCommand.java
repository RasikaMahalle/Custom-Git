package com.mygit.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;

public class RevertCommand {
    private final ObjectStore store = ObjectStore.openDefault();
    private final TreeReader reader = new TreeReader(store);

    public void run(String commitSha) {
        StatusCommand status = new StatusCommand();
        if (status.isDirty()) {
            throw new RuntimeException("Working tree is not clean");
        }

        ObjectStore.ParseObject commit = store.parseObject(store.readObject(commitSha));

        if (!"commit".equals(commit.type)) {
            throw new RuntimeException("Not a commit: " + commitSha);
        }

        String parentSha = extractParent(new String(commit.data));
        if (parentSha == null) {
            throw new RuntimeException("Cannot revert to initial commit.");
        }

        Map<String, String> parentFiles = loadTreeFiles(parentSha);
        Map<String, String> commitFiles = loadTreeFiles(commitSha);

        for (String file : commitFiles.keySet()) {
            if (!parentFiles.containsKey(file)) {
                deleteFile(file);
            }
        }

        for (Map.Entry<String, String> e : parentFiles.entrySet()) {
            restoreBlob(e.getKey(), e.getValue());
        }

        new CommitCommand().run("User <user@mygit>", "Revert " + commitSha);

        System.out.println("Reverted to " + commitSha);
    }

    private Map<String, String> loadTreeFiles(String commitSha) {
        ObjectStore.ParseObject commit = store.parseObject(store.readObject(commitSha));
        String treeSha = extractTreeSha(new String(commit.data));

        Map<String, String> files = new HashMap<>();
        loadTree(treeSha, "", files);
        return files;
    }

    private void loadTree(String treeSha, String base, Map<String, String> out) {
        List<TreeEntry> entries = reader.readTree(treeSha);
        for (TreeEntry e : entries) {
            String path = base.isEmpty() ? e.name : base + "/" + e.name;
            if (e.isTree()) {
                loadTree(e.shaHex, path, out);
            } else {
                out.put(path, e.shaHex);
            }
        }
    }

    private void restoreBlob(String file, String sha) {
        try {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));
            Path p = Paths.get(file);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.write(p, po.data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(String file) {
        try {
            Files.deleteIfExists(Paths.get(file));
        } catch (Exception e) {
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

    private String extractParent(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("parent ")) {
                return line.substring(7).trim();
            }
        }
        return null;
    }
}
