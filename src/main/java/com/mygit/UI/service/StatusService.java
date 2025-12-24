package com.mygit.UI.service;

import java.util.List;

import com.mygit.UI.model.StatusSnapshot;
import com.mygit.command.StatusCommand;

public class StatusService {

    public StatusSnapshot loadStatus() {

        StatusCommand status = new StatusCommand();

        List<String> staged = status.getStagedFiles();
        List<String> modified = status.getModifiedFiles();
        List<String> untracked = status.getUntrackedFiles();

        return new StatusSnapshot(staged, modified, untracked);
    }
}
