package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import com.mygit.UI.model.RepositoryState;
import com.mygit.UI.service.GitStateService;
import com.mygit.command.ConfigCommand;
import com.mygit.command.InitCommand;
import com.mygit.command.MergeCommand;
import com.mygit.command.StashCommand;
import com.mygit.util.Config;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Paths;

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
    private Label statusLabel;
    @FXML
    private StackPane mainContent;
    @FXML
    private VBox repoOverview;
    @FXML
    private VBox emptyState;

    @FXML
    private void showOverview() {
        mainContent.getChildren().setAll(repoOverview);
    }

    @FXML
    private void refreshRepository() {
        loadRepositoryOverview();
    }

    @FXML
    private void initRepository() {
        try {
            boolean already = Files.exists(Paths.get(".mygit"));
            if (!already) {
                new InitCommand().run();
            }

            if (!already) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Initialized empty MyGit repository in the current directory.");
                alert.showAndWait();
                showConfigDialog();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Repository already exists in this directory.");
                alert.showAndWait();
            }

            emptyState.setVisible(false);
            emptyState.setManaged(false);
            repoOverview.setVisible(true);
            repoOverview.setManaged(true);
            mainContent.getChildren().setAll(repoOverview);
            loadRepositoryOverview();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Init failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void stashChanges() {
        try {
            String msg = new StashCommand().push();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.showAndWait();
            loadRepositoryOverview();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Stash failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void applyStash() {
        try {
            String msg = new StashCommand().apply(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.showAndWait();
            loadRepositoryOverview();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Apply stash failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                String rel = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                java.nio.file.Path base = java.nio.file.Paths.get("src/main/java");
                java.nio.file.Path p = base.resolve(rel.replace("/", java.io.File.separator));
                url = p.toUri().toURL();
            }
            FXMLLoader loader = new FXMLLoader(url);
            javafx.scene.Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof BranchController) {
                ((BranchController) controller).setMainController(this);
            }
            mainContent.getChildren().clear();
            mainContent.getChildren().add(node);
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
        boolean repoExists = Files.exists(Paths.get(".mygit"));
        if (repoExists) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            repoOverview.setVisible(true);
            repoOverview.setManaged(true);
            mainContent.getChildren().setAll(repoOverview);
            loadRepositoryOverview();
        } else {
            repoOverview.setVisible(false);
            repoOverview.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            mainContent.getChildren().setAll(emptyState);
            statusLabel.setText("No repository created yet.");
        }
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

    @FXML
    private void createRepoFromEmptyState() {
        initRepository();
    }

    @FXML
    private void showMerge() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Merge");
        dialog.setHeaderText("Merge into current branch");
        dialog.setContentText("Enter branch name or commit SHA to merge:");

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String target = result.get().trim();
        if (target.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Merge target cannot be empty");
            alert.showAndWait();
            return;
        }

        try {
            new MergeCommand().run(target, "User <user@mygit>", null);
            loadRepositoryOverview();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Merge failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void showConfigDialog() {
        try {
            String currentName = Config.userName();
            String currentEmail = Config.userEmail();

            TextInputDialog nameDialog = new TextInputDialog(currentName);
            nameDialog.setTitle("Configure User");
            nameDialog.setHeaderText("Set user.name");
            nameDialog.setContentText("Name:");

            java.util.Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isEmpty()) {
                return;
            }
            String name = nameResult.get().trim();
            if (name.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Name cannot be empty");
                alert.showAndWait();
                return;
            }

            TextInputDialog emailDialog = new TextInputDialog(currentEmail);
            emailDialog.setTitle("Configure User");
            emailDialog.setHeaderText("Set user.email");
            emailDialog.setContentText("Email:");

            java.util.Optional<String> emailResult = emailDialog.showAndWait();
            if (emailResult.isEmpty()) {
                return;
            }
            String email = emailResult.get().trim();
            if (email.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Email cannot be empty");
                alert.showAndWait();
                return;
            }

            ConfigCommand cmd = new ConfigCommand();
            cmd.run(new String[] { "user.name", name });
            cmd.run(new String[] { "user.email", email });

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "User configuration saved.");
            ok.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Config failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void loadRepositoryOverview() {

        RepositoryState state = gitStateService.loadRepositoryState();

        repoPathLabel.setText(state.getRepoPath());
        branchLabel.setText(state.getCurrentBranch());
        headStateLabel.setText(state.isDetachedHead() ? "Detached" : "Attached");
        dirtyStateLabel.setText(state.isDirty() ? "Dirty" : "Clean");

        if (state.getLastCommitSha() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(state.getLastCommitSha());
            String msg = state.getLastCommitMessage();
            if (msg != null && !msg.isBlank()) {
                sb.append(" – ").append(msg);
            }
            String author = state.getLastCommitAuthor();
            if (author != null && !author.isBlank()) {
                sb.append(" | ").append(author);
            }
            String dt = state.getLastCommitDateTime();
            if (dt != null && !dt.isBlank()) {
                sb.append(" | ").append(dt);
            }
            commitLabel.setText(sb.toString());
        } else {
            commitLabel.setText("No commits yet");
        }

        String repoPath = state.getRepoPath();
        if (repoPath == null || repoPath.isEmpty()) {
            statusLabel.setText("No repository loaded");
        } else {
            String branch = state.getCurrentBranch();
            String dirty = state.isDirty() ? "Dirty" : "Clean";
            statusLabel.setText("Repository: " + repoPath + "  |  Branch: " + branch + "  |  " + dirty);
        }
    }
}
