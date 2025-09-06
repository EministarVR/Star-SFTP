package dev.eministar.starsftp.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.converter.IntegerStringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public final class HostDialogFactory {

    public record HostData(String name, String host, int port, String user, String password,
                           boolean saveProfile, boolean rememberSecrets) {}

    private HostDialogFactory() {}

    public static Optional<HostData> showCreateDialog() {
        return showDialog("Neuer Host", null);
    }

    public static Optional<HostData> showEditDialog(HostData initial) {
        return showDialog("Host bearbeiten", initial);
    }

    private static Optional<HostData> showDialog(String title, HostData initial) {
        Dialog<HostData> dialog = new Dialog<>();
        dialog.setTitle(title);

        // Buttons
        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        // Header mit Icon
        Label header = new Label(title);
        header.getStyleClass().add("title");
        header.setGraphic(new FontIcon(Feather.SETTINGS)); // oder SERVER
        header.setGraphicTextGap(8);
        dialog.getDialogPane().setHeader(header);

        // Formular
        TextField tfName = new TextField();
        TextField tfHost = new TextField();
        TextField tfPort = new TextField();
        TextField tfUser = new TextField();
        PasswordField pfPass = new PasswordField();
        CheckBox cbSave = new CheckBox("Speichern");
        CheckBox cbRemember = new CheckBox("Geheimnisse merken (verschlüsselt)");

        tfName.setPromptText("z. B. Prod-SFTP");
        tfHost.setPromptText("116.202.xxx.xxx oder host.name");
        tfPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 22, c -> {
            if (c.getControlNewText().matches("\\d{0,5}")) return c; else return null;
        }));
        tfUser.setPromptText("root");
        pfPass.setPromptText("Passwort");

        // Initialwerte
        if (initial != null) {
            tfName.setText(initial.name());
            tfHost.setText(initial.host());
            tfPort.setText(Integer.toString(initial.port()));
            tfUser.setText(initial.user());
            pfPass.setText(initial.password());
            cbSave.setSelected(initial.saveProfile());
            cbRemember.setSelected(initial.rememberSecrets());
        } else {
            tfPort.setText("22");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 18, 4, 18));

        // Label-Klasse für Styling
        grid.add(label("Name"), 0, 0);      grid.add(tfName, 1, 0);
        grid.add(label("Host"), 0, 1);      grid.add(tfHost, 1, 1);
        grid.add(label("Port"), 0, 2);      grid.add(tfPort, 1, 2);
        grid.add(label("User"), 0, 3);      grid.add(tfUser, 1, 3);
        grid.add(label("Passwort"), 0, 4);  grid.add(pfPass, 1, 4);
        grid.add(label("Speichern"), 0, 5); grid.add(cbSave, 1, 5);
        grid.add(new Label(), 0, 6);        grid.add(cbRemember, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // --- WICHTIG: Styles anwenden ---
        // Globales App-Stylesheet auch auf den Dialog laden
        String css = HostDialogFactory.class.getResource("/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);

        // Buttons stylen
        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        Node cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
        okBtn.getStyleClass().add("primary");
        cancelBtn.getStyleClass().add("neutral");

        // Validation (leicht)
        okBtn.disableProperty().bind(
                tfHost.textProperty().isEmpty()
                        .or(tfUser.textProperty().isEmpty())
                        .or(tfPort.textProperty().isEmpty())
        );

        dialog.setResultConverter(bt -> {
            if (bt == okType) {
                int port = parseInt(tfPort.getText(), 22);
                return new HostData(
                        tfName.getText().trim(),
                        tfHost.getText().trim(),
                        port,
                        tfUser.getText().trim(),
                        pfPass.getText(),
                        cbSave.isSelected(),
                        cbRemember.isSelected()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
