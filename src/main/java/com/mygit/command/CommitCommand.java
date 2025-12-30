package com.mygit.command;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.mygit.RefsUtil;
import com.mygit.index.Index;
import com.mygit.index.IndexEntry;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.TreeWriter;

public class CommitCommand {
    private final ObjectStore store;
    private final Index index;

    public CommitCommand() {
        this.store = ObjectStore.openDefault();
        this.index = new Index(Paths.get(".mygit"));
    }

    public String run(String author, String message) {

        try {
            index.load(); // ✅ ALWAYS reload
        } catch (Exception e) {
            throw new RuntimeException("Failed to load index", e);
        }

        List<IndexEntry> entries = new ArrayList<>(index.getEntries());
        TreeWriter tw = new TreeWriter(store);
        String treeSha = tw.writeTreeFromIndex(entries);

        String parent = RefsUtil.readHEAD();
        String commitSha = store.writeCommit(treeSha, parent, author, message);

        RefsUtil.updateHEADWithSha(commitSha);

        System.out.println("Committed: " + commitSha);
        return commitSha;
    }
}
