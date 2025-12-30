package com.mygit.UI.service;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.mygit.RefsUtil;
import com.mygit.command.StatusCommand;
import com.mygit.storage.ObjectStore;
import com.mygit.UI.model.RepositoryState;

public class GitStateService {

    public RepositoryState loadRepositoryState() {

        String repoPath = Paths.get("").toAbsolutePath().toString();

        String branch = RefsUtil.currentBranch();
        boolean detached = RefsUtil.isDetached();

        StatusCommand status = new StatusCommand();
        boolean dirty = status.isDirty();

        String headSha = RefsUtil.readHEAD();
        String shortSha = null;
        String commitMsg = null;
        String author = null;
        String dateTime = null;

        if (headSha != null) {
            shortSha = headSha.length() > 7 ? headSha.substring(0, 7) : headSha;
            try {
                ObjectStore store = ObjectStore.openDefault();
                ObjectStore.ParseObject po = store.parseObject(store.readObject(headSha));
                if ("commit".equals(po.type)) {
                    String body = new String(po.data);
                    String[] parts = body.split("\n\n", 2);
                    if (parts.length == 2) {
                        commitMsg = parts[1].trim();
                    }
                    String authorLine = null;
                    for (String line : body.split("\n")) {
                        if (line.startsWith("author ")) {
                            authorLine = line;
                            break;
                        }
                    }
                    if (authorLine != null) {
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
                        if (epochStr != null) {
                            int idx = authorLine.lastIndexOf(" " + epochStr);
                            String namePart = authorLine.substring(7, idx).trim();
                            author = namePart;
                            long epoch = Long.parseLong(epochStr);
                            dateTime = DateTimeFormatter
                                    .ofPattern("yyyy-MM-dd HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(Instant.ofEpochSecond(epoch));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        return new RepositoryState(
                repoPath,
                branch != null ? branch : "(detached)",
                detached,
                dirty,
                shortSha,
                commitMsg,
                author,
                dateTime);
    }
}
