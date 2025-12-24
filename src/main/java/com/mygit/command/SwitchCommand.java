package com.mygit.command;

import com.mygit.RefsUtil;

public class SwitchCommand {

    public void run(String branch) {
        run(new String[] { branch });
    }

    public void run(String[] args) {

        if (args.length == 0) {
            throw new RuntimeException("Usage: mygit switch <branch> | mygit switch -create <branch>");
        }
        boolean create = false;
        String branch;

        if ("-create".equals(args[0])) {
            if (args.length != 2) {
                throw new RuntimeException("Usage: mygit switch -create <branch>");
            }
            create = true;
            branch = args[1];
        } else {
            branch = args[0];
        }

        if (create) {
            if (RefsUtil.listBranches().contains(branch)) {
                throw new RuntimeException("Branch already exists.");
            }
            RefsUtil.createBranch(branch);
        } else {
            if (!RefsUtil.listBranches().contains(branch)) {
                throw new RuntimeException("Branch does not exist.");
            }
        }
        new CheckOutCommand().run(branch);
    }
}
