package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import com.mygit.UI.model.CommitViewModel;
import com.mygit.UI.service.GitLogService;

public class LogController {

    @FXML
    private ListView<CommitViewModel> commitList;

    private final GitLogService service = new GitLogService();

    @FXML
    private void initialize() {

        service.loadLog().forEach(c ->
            commitList.getItems().add(
                new CommitViewModel(
                    c.sha,
                    c.message,
                    c.author,
                    c.date
                )
            )
        );
    }
}
