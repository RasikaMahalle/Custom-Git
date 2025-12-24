package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import com.mygit.UI.model.RepositoryState;
import com.mygit.UI.service.GitStateService;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private Label repoPathLabel;
    @FXML
    private Label branchLabel;
    @FXML
    private Label headStateLabel;
    @FXML
    private Label dirtyStateLabel;
    @FXML
    private Label commitLabel;
    @FXML
    private StackPane mainContent;
    @FXML
    private VBox repoOverview;

    @FXML
    private void showOverview() {
        mainContent.getChildren().setAll(repoOverview);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath));
            mainContent.getChildren().add(loader.load());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }

    @FXML
    private void showLog() {
        loadView("/com/mygit/UI/view/log.fxml");
    }

    @FXML
    private void showBranches() {
        loadView("/com/mygit/UI/view/branches.fxml");
    }

    private final GitStateService gitStateService = new GitStateService();

    @FXML
    public void initialize() {
        loadRepositoryOverview();
    }

    @FXML
    private void showStatus() {
        loadView("/com/mygit/UI/view/status.fxml");
    }

    @FXML
    private void showDiff() {
        loadView("/com/mygit/UI/view/diff.fxml");
    }

    @FXML
    private void showCommit() {
        loadView("/com/mygit/UI/view/commit.fxml");
    }

    private void loadRepositoryOverview() {

        RepositoryState state = gitStateService.loadRepositoryState();

        repoPathLabel.setText(state.getRepoPath());
        branchLabel.setText(state.getCurrentBranch());
        headStateLabel.setText(state.isDetachedHead() ? "Detached" : "Attached");
        dirtyStateLabel.setText(state.isDirty() ? "Dirty" : "Clean");

        if (state.getLastCommitSha() != null) {
            commitLabel.setText(
                    state.getLastCommitSha() + " – " +
                            (state.getLastCommitMessage() != null
                                    ? state.getLastCommitMessage()
                                    : ""));
        } else {
            commitLabel.setText("No commits yet");
        }
    }
}
