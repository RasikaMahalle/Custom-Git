package com.mygit.UI.service;

import java.nio.file.Paths;

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

        if (headSha != null) {
            shortSha = headSha.length() > 7 ? headSha.substring(0, 7) : headSha;
            commitMsg = readCommitMessage(headSha);
        }

        return new RepositoryState(
                repoPath,
                branch != null ? branch : "(detached)",
                detached,
                dirty,
                shortSha,
                commitMsg);
    }

    private String readCommitMessage(String commitSha) {
        try {
            ObjectStore store = ObjectStore.openDefault();
            ObjectStore.ParseObject po = store.parseObject(store.readObject(commitSha));

            if (!"commit".equals(po.type)) {
                return null;
            }

            String body = new String(po.data);
            String[] parts = body.split("\n\n", 2);
            return parts.length == 2 ? parts[1].trim() : null;

        } catch (Exception e) {
            return null;
        }
    }
}
