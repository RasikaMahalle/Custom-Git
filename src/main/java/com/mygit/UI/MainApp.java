package com.mygit.UI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        java.net.URL fxml = getClass().getResource("/com/mygit/UI/view/main.fxml");
        if (fxml == null) {
            java.nio.file.Path p = java.nio.file.Paths.get("src/main/java/com/mygit/UI/view/main.fxml");
            fxml = p.toUri().toURL();
        }
        FXMLLoader loader = new FXMLLoader(fxml);

        Scene scene = new Scene(loader.load(), 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
