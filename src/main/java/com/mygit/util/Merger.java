package com.mygit.util;

import java.io.ByteArrayOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import com.mygit.RefsUtil;
import com.mygit.command.CheckOutCommand;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeReader;
import com.mygit.storage.TreeWriter;
import com.mygit.storage.TreeReader.TreeEntry;

public class Merger {
    private final ObjectStore store;
    private final TreeReader tr;
    private final MergeUtils utils;

    public Merger(ObjectStore store) {
        this.store = store;
        this.tr = new TreeReader(store);
        this.utils = new MergeUtils(store);
    }

    public static class Pair {
        public final String mode;
        public final String shaHex;

        public Pair(String mode, String shaHex) {
            this.mode = mode;
            this.shaHex = shaHex;
        }
    }

    public Map<String, Pair> buildPathMap(String treeSha) {
        Map<String, Pair> map = new HashMap<>();
        walkTree(treeSha, "", map);
        return map;
    }

    private void walkTree(String treeSha, String prefix, Map<String, Pair> map) {
        if (treeSha == null)
            return;
        List<TreeEntry> entries = tr.readTree(treeSha);
        for (TreeEntry e : entries) {
            String path = prefix.isEmpty() ? e.name : (prefix + "/" + e.name);
            if (e.isTree()) {
                walkTree(e.shaHex, path, map);
            } else {
                map.put(path, new Pair(e.mode, e.shaHex));
            }
        }
    }

    // Merge
    public MergeResult merge(String ourCommitSha, String theirCommitSha, String author, String message) {
        String base = utils.findLCA(ourCommitSha, theirCommitSha);
        if (base == null) {
            throw new RuntimeException("No common ancestors found between " + ourCommitSha + " and " + theirCommitSha);
        }

        String ourTree = utils.getTreeSha(ourCommitSha);
        String theirTree = utils.getTreeSha(theirCommitSha);
        String baseTree = utils.getTreeSha(base);

        Map<String, Pair> baseMap = baseTree == null ? Map.of() : buildPathMap(baseTree);
        Map<String, Pair> ourMap = ourTree == null ? Map.of() : buildPathMap(ourTree);
        Map<String, Pair> theirMap = theirTree == null ? Map.of() : buildPathMap(theirTree);

        Set<String> allPaths = new TreeSet<>();
        allPaths.addAll(baseMap.keySet());
        allPaths.addAll(ourMap.keySet());
        allPaths.addAll(theirMap.keySet());

        Map<String, Pair> result = new TreeMap<>();
        List<String> conflicts = new ArrayList<>();

        for (String path : allPaths) {
            Pair baseP = baseMap.get(path);
            Pair ourP = ourMap.get(path);
            Pair theirP = theirMap.get(path);

            String baseSha = baseP == null ? null : baseP.shaHex;
            String ourSha = ourP == null ? null : ourP.shaHex;
            String theirSha = theirP == null ? null : theirP.shaHex;

            if (Objects.equals(ourSha, theirSha)) {
                if (ourSha != null)
                    result.put(path, ourP);
                continue;
            }
            if (Objects.equals(ourSha, baseSha) && !Objects.equals(theirSha, baseSha)) {
                if (theirSha != null)
                    result.put(path, theirP);
                continue;
            }
            if (Objects.equals(theirSha, baseSha) && !Objects.equals(ourSha, baseSha)) {
                if (ourSha != null)
                    result.put(path, ourP);
                continue;
            }

            conflicts.add(path);

            byte[] ourContent = readBlobContent(ourSha);
            byte[] theirContent = readBlobContent(theirSha);

            boolean ourBinary = isBinary(ourContent);
            boolean theirBinary = isBinary(theirContent);

            byte[] mergedBytes;

            if (ourBinary || theirBinary) {
                String placeHolder = "<<<<< OURS (binary)\n"
                        + (ourSha == null ? "<deleted>\n" : "Blob: " + ourSha + "\n") + "=====\n"
                        + (theirSha == null ? "<deleted>\n" : "Blob: " + theirSha + "\n") + ">>>>> THEIRS (binary)\n";
                mergedBytes = placeHolder.getBytes();
            } else {
                mergedBytes = conflictMerge(ourContent, theirContent);
            }

            String mergedSha = store.writeBlob(mergedBytes);

            String mode = ourP != null ? ourP.mode : (theirP != null ? theirP.mode : "100644");
            result.put(path, new Pair(mode, mergedSha));
        }

        // Write tree from index is fine, but we need to update the ACTUAL index file 
        // with the new file stats so that subsequent status checks are clean.
        // The current implementation creates a dummy index in memory to write the tree,
        // but it doesn't update the on-disk index with correct timestamps.

        // 1. Write content to working directory
        for (Map.Entry<String, Pair> e : result.entrySet()) {
             String path = e.getKey();
             String sha = e.getValue().shaHex;
             
             // In a real implementation we would only update changed files
             // For now, overwrite to ensure consistency
             byte[] content = readBlobContent(sha);
             Path p = Paths.get(path);
             try {
                 if (p.getParent() != null) Files.createDirectories(p.getParent());
                 Files.write(p, content);
             } catch (Exception ex) {
                 throw new RuntimeException("Failed to write file during merge: " + path, ex);
             }
             
             if ("100755".equals(e.getValue().mode)) {
                 p.toFile().setExecutable(true);
             }
        }

        // 2. Rebuild the index with fresh stats
        com.mygit.index.Index realIndex = new com.mygit.index.Index(Paths.get(".mygit"));
        realIndex.clear();
        
        List<IndexEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Pair> e : result.entrySet()) {
            String pathStr = e.getKey();
            Path path = Paths.get(pathStr);
            com.mygit.util.FileStat stat = com.mygit.util.FileStat.fromPath(path);

            IndexEntry ie = new IndexEntry();
            ie.path = pathStr;
            ie.ctimeSecs = (int) (stat.ctime / 1000);
            ie.ctimeNsecs = (int) (stat.ctime % 1000) * 1000000;
            ie.mtimeSecs = (int) (stat.mtime / 1000);
            ie.mtimeNsecs = (int) (stat.mtime % 1000) * 1000000;
            ie.dev = 0;
            ie.ino = 0;
            try {
                ie.mode = Integer.parseInt(e.getValue().mode, 8);
            } catch (Exception ex) {
                ie.mode = 0100644;
            }
            ie.uid = 0;
            ie.gid = 0;
            ie.fileSize = (int) stat.size;
            ie.sha1 = HashUtils.hexToBytes(e.getValue().shaHex);
            ie.flags = (short) (ie.path.length() & 0x0FFF);
            
            entries.add(ie);
            realIndex.addEntry(ie);
        }
        
        try {
            realIndex.write();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write index during merge", ex);
        }

        TreeWriter tw = new TreeWriter(store);
        String newTreeSha = tw.writeTreeFromIndex(entries);

        if (!conflicts.isEmpty()) {
            // Restore HEAD state if conflicts (or leave conflict markers? Git leaves markers)
            // The prompt implies we should leave markers. 
            // My previous code returned null conflicts if success.
            // But if conflicts exist, we shouldn't commit.
            
            // Wait, the previous logic was:
            // if (!conflicts.isEmpty()) {
            //    new CheckOutCommand().run(ourCommitSha); // This undoes the merge work!
            //    return new MergeResult(null, conflicts);
            // }
            
            // If we want to support conflict resolution, we should leave the files with markers
            // and NOT revert to HEAD.
            // But for now, let's just fix the "dirty after successful merge" issue.
            // The issue is that successful merge writes a commit, updates HEAD, 
            // BUT relies on CheckOutCommand to update working tree?
            // Line 178: new CheckOutCommand().run(mergeCommitSha);
            
            // CheckOutCommand logic:
            // 1. Checks safety (is dirty?) -> fails because we just modified files!
            // OR
            // 2. Updates working tree.
            
            // Actually, we just updated the working tree manually above!
            // And we updated the index manually above!
            // So we don't need to run CheckOutCommand again if we did it right.
            // Running CheckOutCommand(mergeCommitSha) is redundant and might cause the dirty check failure if the index isn't perfectly synced yet.
            
            // However, CheckOutCommand updates the working tree. 
            // If I remove the manual write above, and rely on CheckOutCommand:
            // 1. Write merge commit (using tree from memory index)
            // 2. Update HEAD ref
            // 3. Checkout HEAD
            
            // The problem is "Tree from memory index" (lines 141-164 in original)
            // The memory index entries have 0 timestamps.
            // So the tree is valid, but we haven't updated the on-disk index file.
            // Then CheckOutCommand runs.
            // CheckOutCommand does:
            // - Safety check: compares HEAD vs Work.
            //   HEAD is now the NEW merge commit.
            //   Work is... potentially old (if we didn't write) or new (if we wrote).
            //   Wait, we didn't write files yet in original code!
            
            // Original code:
            // 1. Calculate result map (sha/mode)
            // 2. Create in-memory index entries (timestamps 0)
            // 3. Write tree from these entries -> newTreeSha
            // 4. Write merge commit -> mergeCommitSha
            // 5. Update HEAD to mergeCommitSha
            // 6. Run CheckOutCommand(mergeCommitSha)
            
            // Inside CheckOutCommand(mergeCommitSha):
            // 1. Target = mergeCommitSha
            // 2. Safety Check:
            //    - StatusCommand().getModifiedTrackedFiles()
            //    - StatusCommand compares Index vs Work.
            //      Index is... UNTOUCHED (still has old commit state).
            //      Work is... UNTOUCHED (still has old commit files).
            //      So Status should be clean (assuming we started clean).
            // 3. updateWorkingTree(currentTree, targetTree)
            //    - currentTree = HEAD tree (which is now mergeCommitSha!)
            //    - targetTree = mergeCommitSha
            //    - They are IDENTICAL.
            //    - So updateWorkingTree does NOTHING.
            
            // RESULT: HEAD moved, but files and index NOT updated!
            // Then user runs "switch test".
            // SwitchCommand checks status.
            // Index has OLD state.
            // HEAD has NEW state.
            // Work has OLD state.
            // Status:
            //  - Index vs Head: Differences (Staged changes showing revert to old state?)
            //  - Work vs Index: Clean (timestamps match old state)
            //  - Effectively, it looks like we have staged changes undoing the merge?
            //  - Or if HEAD != Index, StatusCommand reports changes.
            
            // FIX:
            // We MUST update the Index and Working Directory during the merge.
            // Since we already calculated the merge result, we should write it out.
            
            return new MergeResult(null, conflicts);
        }

        String mergeCommitSha = writeMergeCommit(newTreeSha, ourCommitSha, theirCommitSha, author, message);
        RefsUtil.updateHEADWithSha(mergeCommitSha);
        
        // We have already written the files and updated the index above.
        // So we don't need CheckOutCommand.
        // new CheckOutCommand().run(mergeCommitSha); 

        return new MergeResult(mergeCommitSha, new ArrayList<>());
    }

    private String writeMergeCommit(String treeSha, String parentA, String parentB, String author, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeSha).append("\n");
        sb.append("parent ").append(parentA).append("\n");
        sb.append("parent ").append(parentB).append("\n");
        long epoch = System.currentTimeMillis() / 1000;
        String offset = java.time.ZonedDateTime.now().getOffset().getId().replace(":", "");
        sb.append("author ").append(author).append(" ").append(epoch).append(" ").append(offset).append("\n");
        sb.append("committer ").append(author).append(" ").append(epoch).append(" ").append(offset).append("\n");
        sb.append("\n");
        sb.append(message).append("\n");

        byte[] payload = sb.toString().getBytes();
        String header = "commit " + payload.length + "\0";
        byte[] store = concat(header.getBytes(), payload);

        try {
            byte[] shaBytes = HashUtils.sha1(store);
            String shaHex = HashUtils.toHex(shaBytes);
            byte[] compressed = IOUtils.zlibCompress(store);
            Path objPath = Paths.get(".mygit").resolve("objects").resolve(shaHex.substring(0, 2))
                    .resolve(shaHex.substring(2));
            if (!Files.exists(objPath)) {
                Files.createDirectories(objPath.getParent());
                Path tmp = objPath.getParent().resolve(".tmp-" + UUID.randomUUID().toString());
                Files.write(tmp, compressed);
                try {
                    Files.move(tmp, objPath, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp, objPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return shaHex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write merge commit", e);
        }
    }

    private byte[] concat(byte[] a, byte[] b) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(a);
            out.write(b);

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readBlobContent(String shaHex) {
        if (shaHex == null)
            return new byte[0];
        byte[] storeBytes = store.readObject(shaHex);
        ObjectStore.ParseObject po = store.parseObject(storeBytes);
        if (!"blob".equals(po.type)) {
            return new byte[0];
        }
        return po.data;
    }

    private byte[] conflictMerge(byte[] ours, byte[] theirs) {
        String oursText = safeString(ours);
        String theirsText = safeString(theirs);
        StringBuilder sb = new StringBuilder();
        sb.append("<<<<<<< OURS\n");
        sb.append(oursText);
        if (!oursText.endsWith("\n"))
            sb.append("\n");
        sb.append("=======\n");
        sb.append(theirsText);
        if (!theirsText.endsWith("\n"))
            sb.append("\n");
        sb.append(">>>>>>> THEIRS\n");
        return sb.toString().getBytes();
    }

    private String safeString(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (Exception ex) {
            return HashUtils.toHex(HashUtils.sha1(b));
        }
    }

    private boolean isBinary(byte[] b) {
        if (b == null || b.length == 0)
            return false;
        int nonPrintable = 0;
        for (byte ch : b) {
            int v = ch & 0xFF;
            if (v == 0)
                return true;
            // consider tab(9), CR(13), LF(10) and printable 32-126 as text
            if (v == 9 || v == 10 || v == 13)
                continue;
            if (v < 32 || v > 126)
                nonPrintable++;
        }
        double ratio = (double) nonPrintable / (double) b.length;
        return ratio > 0.3;
    }
}
