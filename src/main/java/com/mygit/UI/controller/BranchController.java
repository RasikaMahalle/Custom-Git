package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;

import com.mygit.RefsUtil;
import com.mygit.command.SwitchCommand;
import com.mygit.command.BranchCommand;

import java.util.List;
import java.util.Optional;

public class BranchController {

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
            new BranchCommand().run("create", name.trim());
            refreshBranches();
        });
    }

    @FXML
    private void switchBranch() {
        String selected = branchList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String branch = selected.replace("*", "").trim();
        new SwitchCommand().run(branch);
        refreshBranches();
    }

    @FXML
    private void deleteBranch() {
        String selected = branchList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String branch = selected.replace("*", "").trim();

        if (branch.equals(RefsUtil.currentBranch())) {
            throw new RuntimeException("Cannot delete current branch");
        }

        new BranchCommand().run("delete", branch);
        refreshBranches();
    }
}
