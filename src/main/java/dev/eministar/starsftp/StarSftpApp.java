package dev.eministar.starsftp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class StarSftpApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppNavigator.init(stage);
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/ui/login-view.fxml"), "missing /ui/login-view.fxml"));

        Scene scene = new Scene(root, 1100, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/ui/app.css"), "missing /ui/app.css").toExternalForm());
        stage.setTitle("STAR-SFTP — Login");
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
