package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import com.mygit.command.StatusCommand;

public class StatusController {

    @FXML
    private ListView<String> stagedList;

    @FXML
    private ListView<String> modifiedList;

    @FXML
    private ListView<String> untrackedList;

    @FXML
    private void initialize() {

        StatusCommand status = new StatusCommand();

        stagedList.getItems().addAll(status.getStagedFiles());
        modifiedList.getItems().addAll(status.getModifiedFiles());
        untrackedList.getItems().addAll(status.getUntrackedFiles());
    }
}
