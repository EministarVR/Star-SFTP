package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.model.Profile;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.converter.IntegerStringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public final class HostDialogs {
    private HostDialogs() {}

    public static Optional<Profile> showCreateOrEdit(Profile edit, Class<?> resourceBase) {
        Dialog<Profile> dlg = new Dialog<>();
        dlg.setTitle(edit == null ? "Neuer Host" : "Host bearbeiten");

        DialogPane pane = new DialogPane();
        pane.getButtonTypes().setAll(
                new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        // CSS + undecorated + draggable
        Dialogs.attachAppStyles(pane, resourceBase, Dialogs.DEFAULT_CSS_PATH);
        dlg.setDialogPane(pane);

        // Header (draggable)
        Label header = new Label(dlg.getTitle());
        header.getStyleClass().add("dialog-titlebar");
        header.setGraphic(new FontIcon(Feather.SERVER));
        header.setGraphicTextGap(8);
        pane.setHeader(header);
        Dialogs.undecorateAndMakeDraggable(dlg, header);

        // Grid Layout
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(18, 18, 12, 18));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(92);
        c0.setPrefWidth(110);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(c0, c1);

        // Fields
        TextField tfName = new TextField(edit == null ? "" : edit.name());
        tfName.setPromptText("z. B. Prod-SFTP");
        TextField tfHost = new TextField(edit == null ? "" : edit.host());
        tfHost.setPromptText("host oder IP");
        TextField tfPort = new TextField(String.valueOf(edit == null ? 22 : edit.port()));
        tfPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), (edit == null ? 22 : edit.port()),
                c -> c.getControlNewText().matches("\\d{0,5}") ? c : null));
        TextField tfUser = new TextField(edit == null ? "" : edit.username());
        tfUser.setPromptText("user");
        PasswordField pfPass = new PasswordField();
        pfPass.setText(edit == null ? "" : (edit.password() == null ? "" : edit.password()));
        pfPass.setPromptText("passwort");

        CheckBox chkRemember = new CheckBox("Geheimnisse merken (verschlüsselt)");
        chkRemember.setSelected(edit != null && edit.remember());

        // Add rows
        int r = 0;
        grid.add(formLabel("Name"), 0, r);      grid.add(tfName, 1, r++); tfName.setPrefWidth(320);
        grid.add(formLabel("Host"), 0, r);      grid.add(tfHost, 1, r++); tfHost.setPrefWidth(320);
        grid.add(formLabel("Port"), 0, r);      grid.add(tfPort, 1, r++); tfPort.setPrefWidth(120);
        grid.add(formLabel("User"), 0, r);      grid.add(tfUser, 1, r++); tfUser.setPrefWidth(220);
        grid.add(formLabel("Passwort"), 0, r);  grid.add(pfPass, 1, r++); pfPass.setPrefWidth(220);
        grid.add(formLabel("Speichern"), 0, r); grid.add(chkRemember, 1, r++);

        pane.setContent(grid);

        // Button styles
        ButtonType okType = pane.getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElse(null);
        ButtonType cancelType = pane.getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE).findFirst().orElse(null);
        Dialogs.styleButtons(pane, okType, cancelType);

        Node okBtn = pane.lookupButton(okType);
        if (okBtn != null) {
            okBtn.disableProperty().bind(
                    tfHost.textProperty().isEmpty()
                            .or(tfUser.textProperty().isEmpty())
                            .or(tfPort.textProperty().isEmpty())
            );
        }

        dlg.setResultConverter(bt -> {
            if (bt == okType) {
                int port;
                try { port = Integer.parseInt(tfPort.getText().trim()); }
                catch (Exception e) { return null; }
                return new Profile(
                        tfName.getText().trim(),
                        tfHost.getText().trim(),
                        port,
                        tfUser.getText().trim(),
                        pfPass.getText().isBlank() ? null : pfPass.getText(),
                        null,
                        chkRemember.isSelected()
                );
            }
            return null;
        });

        return dlg.showAndWait();
    }

    private static Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }
}
