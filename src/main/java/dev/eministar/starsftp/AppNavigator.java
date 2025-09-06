package dev.eministar.starsftp;

import dev.eministar.starsftp.model.Profile;
import dev.eministar.starsftp.sftp.SftpService;
import dev.eministar.starsftp.ui.SftpController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.Consumer;

public final class AppNavigator {
    private static Stage stage;

    public static void init(Stage primary) {
        stage = primary;
        stage.setMinWidth(960);
        stage.setMinHeight(620);
    }

    public static void showLogin() {
        show("/ui/login-view.fxml", "STAR-SFTP — Login", 960, 640, null);
    }

    public static void showSftp(SftpService sftp, Profile profile) {
        show("/ui/sftp-view.fxml", "STAR-SFTP", 1180, 720, (SftpController c) -> c.init(sftp, profile));
    }

    private static <C> void show(String fxml, String title, int w, int h, Consumer<C> init) {
        try {
            FXMLLoader l = new FXMLLoader(Objects.requireNonNull(
                    AppNavigator.class.getResource(fxml), "missing " + fxml));
            Parent root = l.load();
            @SuppressWarnings("unchecked") C controller = l.getController();
            if (init != null && controller != null) init.accept(controller);

            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(Objects.requireNonNull(
                    AppNavigator.class.getResource("/ui/app.css"), "missing /ui/app.css").toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("View load fail: " + fxml, e);
        }
    }
}
