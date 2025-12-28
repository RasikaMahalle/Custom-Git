package com.mygit.UI.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.mygit.RefsUtil;
import com.mygit.storage.ObjectStore;

public class GitLogService {

    private final ObjectStore store = ObjectStore.openDefault();

    public List<CommitData> loadLog() {

        List<CommitData> commits = new ArrayList<>();
        String current = RefsUtil.readHEAD();

        while (current != null) {
            ObjectStore.ParseObject po = store.parseObject(store.readObject(current));

            if (!"commit".equals(po.type))
                break;

            CommitData data = CommitData.fromCommitBody(current, new String(po.data));
            commits.add(data);

            current = data.parent;
        }

        return commits;
    }

    // ---------- Inner DTO ----------
    public static class CommitData {
        public String sha;
        public String message;
        public String author;
        public String date;
        public String parent;

        static CommitData fromCommitBody(String sha, String body) {
            CommitData d = new CommitData();
            d.sha = sha.substring(0, 7);

            for (String line : body.split("\n")) {
                if (line.startsWith("parent ")) {
                    d.parent = line.substring(7).trim();
                } else if (line.startsWith("author ")) {
                    d.author = line.substring(7);
                }
            }

            d.message = body.substring(body.indexOf("\n\n") + 2).trim();

            String authorLine = null;
            for (String line : body.split("\n")) {
                if (line.startsWith("author ")) {
                    authorLine = line;
                    break;
                }
            }
            if (authorLine == null) {
                throw new RuntimeException("No author line in commit");
            }

            String[] tokens = authorLine.split(" ");
            String epochStr = null;
            for (int i = tokens.length - 1; i >= 1; i--) {
                String t = tokens[i];
                if (t.matches("^\\d+$")) {
                    epochStr = t;
                    break;
                }
                int plus = t.indexOf('+', 1);
                int minus = t.indexOf('-', 1);
                int idx = plus > 0 ? plus : (minus > 0 ? minus : -1);
                if (idx > 0) {
                    String candidate = t.substring(0, idx);
                    if (candidate.matches("^\\d+$")) {
                        epochStr = candidate;
                        break;
                    }
                }
            }
            if (epochStr == null) {
                throw new RuntimeException("Cannot parse author timestamp: " + authorLine);
            }

            long epoch = Long.parseLong(epochStr);

            d.date = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(epoch));

            return d;
        }
    }
}
