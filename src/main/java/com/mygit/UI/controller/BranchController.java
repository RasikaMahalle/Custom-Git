package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;

import com.mygit.RefsUtil;
import com.mygit.command.SwitchCommand;
import com.mygit.command.BranchCommand;

import java.util.List;
import java.util.Optional;

public class BranchController {

    private MainController mainController;

    @FXML
    private ListView<String> branchList;

    @FXML
    public void initialize() {
        refreshBranches();
    }

    private void refreshBranches() {
        branchList.getItems().clear();

        String current = RefsUtil.currentBranch();
        List<String> branches = RefsUtil.listBranches();

        for (String b : branches) {
            if (b.equals(current)) {
                branchList.getItems().add("* " + b);
            } else {
                branchList.getItems().add("  " + b);
            }
        }
    }

    @FXML
    private void createBranch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Branch");
        dialog.setHeaderText("Enter new branch name");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                new BranchCommand().run("create", name.trim());
                refreshBranches();
                if (mainController != null) {
                    mainController.loadRepositoryOverview();
                }
            } catch (Exception e) {
                showError("Create Branch Failed", e.getMessage());
            }
        });
    }

    @FXML
    private void switchBranch() {
        String selected = branchList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String branch = selected.replace("*", "").trim();
        try {
            new SwitchCommand().run(branch);
            refreshBranches();
            if (mainController != null) {
                mainController.loadRepositoryOverview();
            }
        } catch (Exception e) {
            showError("Switch Branch Failed", e.getMessage());
        }
    }

    @FXML
    private void deleteBranch() {
        String selected = branchList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String branch = selected.replace("*", "").trim();

        if (branch.equals(RefsUtil.currentBranch())) {
            showError("Delete Branch Failed", "Cannot delete current branch");
            return;
        }

        try {
            new BranchCommand().deleteBranch(branch);
            refreshBranches();
            if (mainController != null) {
                mainController.loadRepositoryOverview();
            }
        } catch (Exception e) {
            showError("Delete Branch Failed", e.getMessage());
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
