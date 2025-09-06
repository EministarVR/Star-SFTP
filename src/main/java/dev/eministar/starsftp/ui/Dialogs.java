package dev.eministar.starsftp.ui;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class Dialogs {
    private Dialogs() {}

    public static final String DEFAULT_CSS_PATH = "/ui/app.css";

    /** Hängt das App-CSS an die DialogPane (wirft keine NPE, loggt nur Warnung). */
    public static void attachAppStyles(DialogPane pane, Class<?> resourceBase, String cssPath) {
        String path = (cssPath == null || cssPath.isBlank()) ? DEFAULT_CSS_PATH : cssPath;
        URL url = resourceBase.getResource(path);
        if (url != null) {
            pane.getStylesheets().add(url.toExternalForm());
        } else {
            System.err.println("WARN: Stylesheet nicht gefunden: " + path);
        }
    }

    /** Entfernt OS-Deko, setzt nice Shadow-Radius via CSS; macht Header/Node draggable. */
    public static void undecorateAndMakeDraggable(Dialog<?> dlg, Node dragNode) {
        dlg.initStyle(StageStyle.UNDECORATED);
        if (dragNode == null) return;

        final double[] offset = new double[2];
        dragNode.setOnMousePressed(e -> {
            var w = dlg.getDialogPane().getScene().getWindow();
            offset[0] = e.getScreenX() - w.getX();
            offset[1] = e.getScreenY() - w.getY();
        });
        dragNode.setOnMouseDragged(e -> {
            var w = dlg.getDialogPane().getScene().getWindow();
            w.setX(e.getScreenX() - offset[0]);
            w.setY(e.getScreenY() - offset[1]);
        });
    }

    /** Vergibt Style-Klassen für Primär- und Sekundärbutton im Dialog. */
    public static void styleButtons(DialogPane pane, ButtonType primary, ButtonType secondary) {
        if (primary != null) {
            Node ok = pane.lookupButton(primary);
            if (ok != null) ok.getStyleClass().addAll("primary", "btn-md");
        }
        if (secondary != null) {
            Node cancel = pane.lookupButton(secondary);
            if (cancel != null) cancel.getStyleClass().addAll("neutral", "btn-md");
        }
    }

    /** Bestätigungsdialog im App-Style. */
    public static boolean confirm(Window owner, String title, String header, String content,
                                  Class<?> resourceBase, String cssPath) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(header);
        if (owner != null) a.initOwner(owner);
        attachAppStyles(a.getDialogPane(), resourceBase, cssPath);
        // undecorated + draggable über den Header
        undecorateAndMakeDraggable(a, a.getDialogPane().lookup(".header-panel"));
        styleButtons(a.getDialogPane(), ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /** Überladung mit Default-CSS-Pfad. */
    public static boolean confirm(Window owner, String header, String content, Class<?> base) {
        return confirm(owner, header, header, content, base, DEFAULT_CSS_PATH);
    }
}
