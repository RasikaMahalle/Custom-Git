package com.mygit.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.mygit.RefsUtil;
import com.mygit.storage.ObjectStore;
import com.mygit.util.MergeResult;
import com.mygit.util.Merger;

public class MergeCommand {
    private final ObjectStore store;

    public MergeCommand() {
        this.store = ObjectStore.openDefault();
    }

    public void run(String targetRefOrSha, String author, String message) {
        String targetSha;
        Path refPath = Paths.get(".mygit").resolve("refs").resolve("heads").resolve(targetRefOrSha);
        String targetLabel = targetRefOrSha;

        if (Files.exists(refPath)) {
            try {
                targetSha = Files.readString(refPath).trim();
            } catch (Exception e) {
                throw new RuntimeException("Failed reading branch ref: " + targetRefOrSha, e);
            }
        } else {
            targetSha = targetRefOrSha;
            targetLabel = targetRefOrSha.length() > 7 ? targetRefOrSha.substring(0, 7) : targetRefOrSha;
        }

        String ourHead = RefsUtil.readHEAD();
        if (ourHead == null)
            throw new RuntimeException("No HEAD commit to merge into");

        Merger merger = new Merger(store);

        if (message == null || message.isBlank()) {
            String currentBranch = RefsUtil.currentBranch();
            String theirs = targetLabel;
            String ours = currentBranch != null ? currentBranch
                    : (ourHead.length() > 7 ? ourHead.substring(0, 7) : ourHead);
            StringBuilder tmpl = new StringBuilder();
            tmpl.append("Merge branch '").append(theirs).append("' into '").append(ours).append("'\n\n");
            tmpl.append("# Please enter a commit message to describe the changes.\n");
            tmpl.append("# Lines starting with '#' will be ignored.\n");

            message = tmpl.toString();
        }

        MergeResult res = merger.merge(ourHead, targetSha, author, message);

        if (res.conflicts.isEmpty()) {
            System.out.println("Merge commit created: " + res.commitSha);
            System.out.println("No conflicts. Merge successful.");
        } else {
            System.out.println("Merge completed with conflicts in the following paths:");
            for (String c : res.conflicts) {
                System.out.println("  " + c);
            }
            String currentBranch = RefsUtil.currentBranch();
            String theirsLabel = targetLabel;
            String oursLabel = currentBranch != null ? currentBranch
                    : (ourHead.length() > 7 ? ourHead.substring(0, 7) : ourHead);

            StringBuilder suggested = new StringBuilder();
            suggested.append("Merge branch '").append(theirsLabel).append("' into '").append(oursLabel).append("'\n\n");
            suggested.append("# Conflicts:\n");
            for (String c : res.conflicts) {
                suggested.append("# ").append(c).append("\n");
            }
            suggested.append("\n# Please edit the message and remove lines starting with '#' before finalizing.\n");

            System.out.println("\nSuggested commit message template:\n");
            System.out.println(suggested.toString());
            System.out.println("Note: working directory contains conflict markers for the files above.");
        }
    }
    
}
