package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.AppNavigator;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SplashController {
    @FXML private ImageView imgLogo;
    @FXML private ProgressBar bar;
    @FXML private Label dummy;

    public void play() {
        // Logo: sanft einblenden + Scale-Bounce
        imgLogo.setOpacity(0);
        imgLogo.setScaleX(0.7);
        imgLogo.setScaleY(0.7);

        FadeTransition fade = new FadeTransition(Duration.millis(650), imgLogo);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale1 = new ScaleTransition(Duration.millis(650), imgLogo);
        scale1.setFromX(0.7); scale1.setFromY(0.7);
        scale1.setToX(1.04);  scale1.setToY(1.04);

        ScaleTransition settle = new ScaleTransition(Duration.millis(220), imgLogo);
        settle.setFromX(1.04); settle.setFromY(1.04);
        settle.setToX(1.0);    settle.setToY(1.0);

        Timeline fill = new Timeline(
                new KeyFrame(Duration.ZERO,          new KeyValue(bar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(1.1),  new KeyValue(bar.progressProperty(), 0.35)),
                new KeyFrame(Duration.seconds(1.8),  new KeyValue(bar.progressProperty(), 0.65)),
                new KeyFrame(Duration.seconds(2.2),  new KeyValue(bar.progressProperty(), 1.00))
        );

        SequentialTransition seq = new SequentialTransition(
                new ParallelTransition(fade, scale1),
                settle,
                fill,
                // kleiner „Hold“ und dann weiter zum Login
                new PauseTransition(Duration.millis(180))
        );
        seq.setOnFinished(e -> AppNavigator.showLogin());
        seq.play();
    }
}
