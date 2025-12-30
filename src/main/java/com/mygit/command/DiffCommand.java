package com.mygit.command;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.mygit.RefsUtil;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeReader.TreeEntry;
import com.mygit.util.HashUtils;

public class DiffCommand {

    private final ObjectStore store = ObjectStore.openDefault();
    private final TreeReader reader = new TreeReader(store);

    /*
     * ============================================================
     * CLI ENTRY POINTS (unchanged behavior)
     * ============================================================
     */

    public void run() {
        for (String file : getWorkingDiffFiles()) {
            System.out.print(diffWorkingFile(file));
        }
    }

    public void runStaged() {
        for (String file : getStagedDiffFiles()) {
            System.out.print(diffStagedFile(file));
        }
    }

    /*
     * ============================================================
     * UI API (used by DiffController)
     * ============================================================
     */

    // git diff
    public List<String> getWorkingDiffFiles() {

        Map<String, String> headFiles = loadHeadFiles();
        Index index = loadIndex();

        List<String> result = new ArrayList<>();

        for (IndexEntry ie : index.getEntries()) {

            String path = ie.path;
            Path file = Paths.get(path);

            // file must exist in HEAD and working tree
            if (!headFiles.containsKey(path))
                continue;
            if (!Files.exists(file))
                continue;

            try {
                String workingSha = HashUtils.toHex(
                        HashUtils.sha1(Files.readAllBytes(file)));
                String headSha = headFiles.get(path);

                if (!Objects.equals(workingSha, headSha)) {
                    result.add(path);
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    // git diff --staged
    public List<String> getStagedDiffFiles() {
        Map<String, String> headFiles = loadHeadFiles();
        Index index = loadIndex();

        List<String> result = new ArrayList<>();

        for (IndexEntry ie : index.getEntries()) {
            String indexSha = HashUtils.toHex(ie.sha1);
            String headSha = headFiles.get(ie.path);

            if (!Objects.equals(indexSha, headSha)) {
                result.add(ie.path);
            }
        }
        return result;
    }

    public String diffWorkingFile(String file) {
        try {
            String working;
            try {
                working = Files.readString(Paths.get(file));
            } catch (MalformedInputException e) {
                return binaryDiffMessage(file);
            }
            String committed = readHeadBlob(file);
            return buildDiff(file, committed, working);
        } catch (Exception e) {
            throw new RuntimeException("Failed diff (working): " + file, e);
        }
    }

    public String diffStagedFile(String file) {
        try {
            Index index = loadIndex();
            IndexEntry ie = index.find(file);
            if (ie == null)
                return "";

            String indexText;
            try {
                indexText = new String(
                        store.parseObject(
                                store.readObject(HashUtils.toHex(ie.sha1))).data);
            } catch (Exception e) {
                return binaryDiffMessage(file);
            }

            String headText = readHeadBlob(file);
            return buildDiff(file, headText, indexText);
        } catch (Exception e) {
            throw new RuntimeException("Failed diff (staged): " + file, e);
        }
    }

    /*
     * ============================================================
     * INTERNAL HELPERS
     * ============================================================
     */

    private Index loadIndex() {
        Index index = new Index(Paths.get(".mygit"));
        try {
            index.load();
        } catch (Exception ignored) {
        }
        return index;
    }

    private Map<String, String> loadHeadFiles() {
        Map<String, String> map = new HashMap<>();
        String headSha = RefsUtil.readHEAD();
        if (headSha == null)
            return map;

        ObjectStore.ParseObject commit = store.parseObject(store.readObject(headSha));
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

    private String readHeadBlob(String file) {
        Map<String, String> headFiles = loadHeadFiles();
        String sha = headFiles.get(file);
        if (sha == null)
            return "";
        try {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(sha));
            return new String(po.data);
        } catch (Exception e) {
            return "";
        }
    }
    
    private String binaryDiffMessage(String file) {
        StringBuilder sb = new StringBuilder();
        sb.append("diff -- ").append(file).append("\n");
        sb.append("--- a/").append(file).append("\n");
        sb.append("+++ b/").append(file).append("\n");
        sb.append("Binary file, diff not supported.\n\n");
        return sb.toString();
    }

    private String extractTreeSha(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("tree ")) {
                return line.substring(5).trim();
            }
        }
        throw new RuntimeException("Commit has no tree");
    }

    /*
     * ============================================================
     * DIFF GENERATOR
     * ============================================================
     */

    private String buildDiff(String file, String oldText, String newText) {
        List<String> oldLines = oldText.lines().collect(Collectors.toList());
        List<String> newLines = newText.lines().collect(Collectors.toList());

        int n = oldLines.size();
        int m = newLines.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        if (dp[n][m] == n && n == m) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("diff -- ").append(file).append("\n");
        sb.append("--- a/").append(file).append("\n");
        sb.append("+++ b/").append(file).append("\n");

        List<String> diffs = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                diffs.add("+ " + newLines.get(j - 1));
                j--;
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                diffs.add("- " + oldLines.get(i - 1));
                i--;
            }
        }
        Collections.reverse(diffs);
        for (String d : diffs) {
            sb.append(d).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}
