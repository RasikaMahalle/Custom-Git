package com.mygit.util;

import java.util.List;

public class MergeResult {
    public final String commitSha;
    public final List<String> conflicts;

    public MergeResult(String commitSha, List<String> conflicts) {
        this.commitSha = commitSha;
        this.conflicts = conflicts;
    }
}
