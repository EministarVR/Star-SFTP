package dev.eministar.starsftp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class StarSftpApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppNavigator.init(stage);

        // Splash laden
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/ui/splash-view.fxml"), "missing /ui/splash-view.fxml"));
        Parent root = loader.load();

        // Styles
        String css = Objects.requireNonNull(
                getClass().getResource("/ui/app.css"), "missing /ui/app.css").toExternalForm();
        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(css);

        // App-Icon (passe Pfad an, falls dein Logo woanders liegt)
        Image icon = null;
        var iconUrl = getClass().getResource("/ui/logo.png");
        if (iconUrl != null) icon = new Image(iconUrl.toExternalForm());
        if (icon != null) stage.getIcons().add(icon);

        stage.setTitle("STAR-SFTP");
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();

        // Splash-Animation starten → ruft am Ende AppNavigator.showLogin()
        dev.eministar.starsftp.ui.SplashController c = loader.getController();
        c.play();
    }

    public static void main(String[] args) { launch(args); }
}
