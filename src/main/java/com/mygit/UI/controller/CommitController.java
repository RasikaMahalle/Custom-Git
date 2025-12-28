package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import com.mygit.command.AddCommand;
import com.mygit.command.CommitCommand;
import com.mygit.command.StatusCommand;

public class CommitController {

    @FXML
    private ListView<String> workingFiles;

    @FXML
    private ListView<String> stagedFiles;

    @FXML
    private TextArea commitMessage;

    private final StatusCommand status = new StatusCommand();

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        workingFiles.getItems().clear();
        stagedFiles.getItems().clear();

        workingFiles.getItems().addAll(status.getModifiedFiles());
        workingFiles.getItems().addAll(status.getUntrackedFiles());
        stagedFiles.getItems().addAll(status.getStagedFiles());
    }

    @FXML
    private void stageSelected() {
        String file = workingFiles.getSelectionModel().getSelectedItem();
        if (file == null)
            return;

        new AddCommand().run(file);
        refresh();
    }

    @FXML
    private void unstageSelected() {
        String file = stagedFiles.getSelectionModel().getSelectedItem();
        if (file == null)
            return;

        // simple unstage = reset file from index
        new com.mygit.command.RestoreCommand().run(file);
        refresh();
    }

    @FXML
    private void commit() {

        String msg = commitMessage.getText().trim();

        if (msg.isEmpty()) {
            throw new RuntimeException("Commit message cannot be empty");
        }

        new CommitCommand().run(
                "User <user@mygit>",
                msg);

        commitMessage.clear();
        refresh();
    }
}
