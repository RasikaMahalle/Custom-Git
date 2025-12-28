package com.mygit.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.mygit.RefsUtil;
import com.mygit.pack.ReachabilityWalker;

public class BranchCommand {
    public void run() {
        List<String> branches = RefsUtil.listBranches();
        String current = RefsUtil.currentBranch();

        for (String b : branches) {
            if (b.equals(current)) {
                System.out.println("* " + b);
            } else {
                System.out.println("  " + b);
            }
        }
    }

    public void run(String action, String name) {
        if (!"create".equals(action)) {
            System.out.println("Usage: branch create <name>");
            return;
        }
        RefsUtil.createBranch(name);
        System.out.println("Created branch: " + name);
    }

    public void deleteBranch(String name) {

        String current = RefsUtil.currentBranch();
        if (name.equals(current)) {
            throw new RuntimeException("Cannot delete current branch: " + name);
        }

        Path branchPath = Paths.get(".mygit/refs/heads").resolve(name);
        if (!Files.exists(branchPath)) {
            throw new RuntimeException("Branch does not exist: " + name);
        }

        String branchCommit;
        try {
            branchCommit = Files.readString(branchPath).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read branch", e);
        }

        String headCommit = RefsUtil.readHEAD();

        ReachabilityWalker reach = new ReachabilityWalker();
        if (!reach.isReachable(headCommit, branchCommit)) {
            throw new RuntimeException("Branch '" + name + "' is not fully merged.\n" +
                    "Use force delete only if you know what you are doing.");
        }
        try {
            Files.delete(branchPath);
            System.out.println("Deleted branch: " + name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete branch", e);
        }
    }

}
