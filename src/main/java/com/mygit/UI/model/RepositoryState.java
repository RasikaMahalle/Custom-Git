package com.mygit.UI.model;

public class RepositoryState {

    private final String repoPath;
    private final String currentBranch;
    private final boolean detachedHead;
    private final boolean dirty;
    private final String lastCommitSha;
    private final String lastCommitMessage;

    public RepositoryState(
            String repoPath,
            String currentBranch,
            boolean detachedHead,
            boolean dirty,
            String lastCommitSha,
            String lastCommitMessage) {

        this.repoPath = repoPath;
        this.currentBranch = currentBranch;
        this.detachedHead = detachedHead;
        this.dirty = dirty;
        this.lastCommitSha = lastCommitSha;
        this.lastCommitMessage = lastCommitMessage;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public boolean isDetachedHead() {
        return detachedHead;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getLastCommitSha() {
        return lastCommitSha;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }
}
