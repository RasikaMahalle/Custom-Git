package com.mygit.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mygit.storage.ObjectStore;

public class MergeUtils {
    private final ObjectStore store;

    public MergeUtils(ObjectStore store) {
        this.store = store;
    }

    public List<String> getParents(String commitSha) {
        byte[] commitBytes = store.readObject(commitSha);
        ObjectStore.ParseObject po = store.parseObject(commitBytes);
        if (!"commit".equals(po.type))
            throw new RuntimeException("Not a commit: " + commitSha);
        String body = new String(po.data);
        List<String> parents = new ArrayList<>();
        for (String line : body.split("\n")) {
            if (line.startsWith("parent "))
                parents.add(line.substring(7).trim());
        }
        return parents;
    }

    public String getTreeSha(String commitSha) {
        byte[] commitBytes = store.readObject(commitSha);
        ObjectStore.ParseObject po = store.parseObject(commitBytes);
        if (!"commit".equals(po.type))
            throw new RuntimeException("Not a commit: " + commitSha);
        String body = new String(po.data);
        for (String line : body.split("\n")) {
            if (line.startsWith("tree "))
                return line.substring(5).trim();
        }
        return null;
    }

    public Map<String, Integer> ancestorsWithDistance(String start) {
        Map<String, Integer> dist = new HashMap<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        dist.put(start, 0);
        q.add(start);

        while (!q.isEmpty()) {
            String cur = q.poll();
            int d = dist.get(cur);
            List<String> parents = getParents(cur);
            for (String parent : parents) {
                if (!dist.containsKey(parent)) {
                    dist.put(parent, d + 1);
                    q.add(parent);
                }
            }
        }
        return dist;
    }

    // LCA :- Least Common Ancestor
    public String findLCA(String a, String b) {
        Map<String, Integer> da = ancestorsWithDistance(a);
        Map<String, Integer> db = ancestorsWithDistance(b);

        String best = null;
        int bestScore = Integer.MAX_VALUE;
        for (String cand : db.keySet()) {
            if (da.containsKey(cand)) {
                int score = da.get(cand) + db.get(cand);
                if (score < bestScore) {
                    best = cand;
                    bestScore = score;
                }
            }
        }
        return best;
    }
}
