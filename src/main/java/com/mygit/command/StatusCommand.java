package com.mygit.command;

import java.nio.charset.StandardCharsets;
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

public class StatusCommand {

    private final Index index;
    private final ObjectStore store;
    private final IgnoreMatcher ignore;

    // ✅ CLASS-LEVEL STATE (this was missing)
    private List<String> staged = new ArrayList<>();
    private List<String> modified = new ArrayList<>();
    private List<String> untracked = new ArrayList<>();
    private List<String> ignored = new ArrayList<>();

    public StatusCommand() {
        this.index = new Index(Paths.get(".mygit"));
        this.store = ObjectStore.openDefault();
        this.ignore = new IgnoreMatcher(Paths.get("").toAbsolutePath());

    }

    public void run() {
        computeStatus();
        printStatus();
    }

    // ================= CORE LOGIC =================

    private void computeStatus() {

        try {
            index.load();
        } catch (Exception ignored) {
        }

        staged.clear();
        modified.clear();
        untracked.clear();
        ignored.clear();

        Map<String, String> headFiles = loadHeadFiles();
        Map<String, IndexEntry> indexFiles = loadIndexFiles();
        Set<String> workFiles = scanWorkingDirectory();

        // staged & modified
        for (String path : indexFiles.keySet()) {

            IndexEntry ie = indexFiles.get(path);
            String indexSha = HashUtils.toHex(ie.sha1);
            String headSha = headFiles.get(path);

            if (!Objects.equals(indexSha, headSha)) {
                staged.add(path);
            }

            Path wp = Paths.get(path);
            if (Files.exists(wp)) {
                try {
                    FileStat stat = FileStat.fromPath(wp);

                    byte[] content = Files.readAllBytes(wp);
                    String workSha = computeBlobSha(content);

                    if (!workSha.equals(indexSha)) {
                        modified.add(path);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // untracked & ignored
        for (String path : workFiles) {
            if (!indexFiles.containsKey(path)) {
                if (ignore.isIgnored(path)) {
                    ignored.add(path);
                } else {
                    untracked.add(path);
                }
            }
        }
    }

    // ================= HELPERS =================

    private Map<String, String> loadHeadFiles() {
        Map<String, String> map = new HashMap<>();
        String head = RefsUtil.readHEAD();
        if (head == null)
            return map;

        try {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(head));

            if (!"commit".equals(po.type))
                return map;

            String body = new String(po.data);
            for (String line : body.split("\n")) {
                if (line.startsWith("tree ")) {
                    walkTree(line.substring(5).trim(), "", map);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private void walkTree(String treeSha, String prefix, Map<String, String> map) {
        List<TreeEntry> entries = new TreeReader(store).readTree(treeSha);
        for (TreeEntry e : entries) {
            String path = prefix.isEmpty() ? e.name : prefix + "/" + e.name;
            if (e.isTree()) {
                walkTree(e.shaHex, path, map);
            } else {
                map.put(path, e.shaHex);
            }
        }
    }

    private Map<String, IndexEntry> loadIndexFiles() {
        Map<String, IndexEntry> map = new HashMap<>();
        for (IndexEntry e : index.getEntries()) {
            map.put(e.path.replace("\\", "/"), e);
        }
        return map;
    }

    private Set<String> scanWorkingDirectory() {
        Set<String> files = new HashSet<>();
        try {
            Path repoRoot = Paths.get("").toAbsolutePath().normalize();
            Path gitDir = repoRoot.resolve(".mygit").normalize();
            Path outDir = repoRoot.resolve("out").normalize();

            Files.walk(repoRoot)
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(gitDir))
                    .filter(p -> !p.startsWith(outDir))
                    .forEach(p -> {
                        String rel = repoRoot.relativize(p)
                                .toString()
                                .replace("\\", "/");
                        files.add(rel);
                    });
        } catch (Exception ignored) {
        }
        return files;
    }

    private String computeBlobSha(byte[] content) {
        String header = "blob " + content.length + "\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] store = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, store, 0, headerBytes.length);
        System.arraycopy(content, 0, store, headerBytes.length, content.length);

        return HashUtils.toHex(HashUtils.sha1(store));
    }

    // ================= OUTPUT =================

    private void printStatus() {

        System.out.println("On branch " +
                (RefsUtil.currentBranch() != null
                        ? RefsUtil.currentBranch()
                        : "(detached HEAD)"));
        System.out.println();

        if (staged.isEmpty() && modified.isEmpty() && untracked.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean");
            return;
        }

        if (!staged.isEmpty()) {
            System.out.println("Changes to be committed:");
            staged.forEach(s -> System.out.println("  staged:   " + s));
            System.out.println();
        }

        if (!modified.isEmpty()) {
            System.out.println("Changes not staged for commit:");
            modified.forEach(m -> System.out.println("  modified: " + m));
            System.out.println();
        }

        if (!untracked.isEmpty()) {
            System.out.println("Untracked files:");
            untracked.forEach(u -> System.out.println("  " + u));
            System.out.println();
        }

        if (!ignored.isEmpty()) {
            System.out.println("Ignored files:");
            ignored.forEach(i -> System.out.println("  " + i));
            System.out.println();
        }
    }

    // ================= PUBLIC API =================

    public List<String> getStagedFiles() {
        computeStatus();
        return staged;
    }

    public List<String> getModifiedFiles() {
        computeStatus();
        return modified;
    }

    public List<String> getUntrackedFiles() {
        computeStatus();
        return untracked;
    }

    public Set<String> getModifiedTrackedFiles() {
        computeStatus();
        Set<String> modifiedFiles = new HashSet<>(modified);
        modifiedFiles.addAll(staged);
        return modifiedFiles;
    }

    // ✅ Git-faithful dirty check
    public boolean isDirty() {
        computeStatus();
        return !staged.isEmpty() || !modified.isEmpty();
    }
}
