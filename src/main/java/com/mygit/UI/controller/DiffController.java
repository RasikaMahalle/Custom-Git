package com.mygit.UI.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import com.mygit.command.DiffCommand;

import java.util.List;

public class DiffController {

    @FXML
    private ListView<String> fileList;

    @FXML
    private TextArea diffArea;

    @FXML
    private CheckBox stagedOnlyCheck;

    private final DiffCommand diffCommand = new DiffCommand();

    @FXML
    public void initialize() {
        loadFiles();
        stagedOnlyCheck.setOnAction(e -> loadFiles());
    }

    private void loadFiles() {
        fileList.getItems().clear();
        diffArea.clear();

        List<String> files = stagedOnlyCheck.isSelected()
                ? diffCommand.getStagedDiffFiles()
                : diffCommand.getWorkingDiffFiles();

        fileList.getItems().addAll(files);
    }

    @FXML
    private void onFileSelected() {
        String file = fileList.getSelectionModel().getSelectedItem();
        if (file == null)
            return;

        try {
            String diff = stagedOnlyCheck.isSelected()
                    ? diffCommand.diffStagedFile(file)
                    : diffCommand.diffWorkingFile(file);

            diffArea.setText(diff);
        } catch (Exception e) {
            diffArea.setText("Failed to generate diff for " + file + ":\n" + e.getMessage());
        }
    }
}
