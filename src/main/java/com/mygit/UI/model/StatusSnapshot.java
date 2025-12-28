package com.mygit.UI.model;

import java.util.List;

public class StatusSnapshot {

    private final List<String> staged;
    private final List<String> modified;
    private final List<String> untracked;

    public StatusSnapshot(
            List<String> staged,
            List<String> modified,
            List<String> untracked) {

        this.staged = staged;
        this.modified = modified;
        this.untracked = untracked;
    }

    public List<String> getStaged() {
        return staged;
    }

    public List<String> getModified() {
        return modified;
    }

    public List<String> getUntracked() {
        return untracked;
    }
}
