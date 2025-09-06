package dev.eministar.starsftp.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public class ShortcutPrefs {

    public enum Action {
        REFRESH, UP, GO, ROOT, HOME,
        MKDIR, RENAME, DELETE, UPLOAD, DOWNLOAD,
        FOCUS_PATH, OPEN_DIR, LOGOUT, NEW_CONNECTION
    }

    private final Map<Action, KeyCombination> map = new EnumMap<>(Action.class);
    private final ObjectMapper om = new ObjectMapper();
    private final File file = new File(System.getProperty("user.home"), ".star-sftp/shortcuts.json");

    public ShortcutPrefs() {
        defaults();
        load();
    }

    private void defaults() {
        map.put(Action.REFRESH, new KeyCodeCombination(KeyCode.F5));
        map.put(Action.UP, new KeyCodeCombination(KeyCode.BACK_SPACE));
        map.put(Action.GO, new KeyCodeCombination(KeyCode.ENTER));
        map.put(Action.ROOT, new KeyCodeCombination(KeyCode.SLASH, KeyCombination.SHIFT_DOWN));
        map.put(Action.HOME, new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        map.put(Action.MKDIR, new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        map.put(Action.RENAME, new KeyCodeCombination(KeyCode.F2));
        map.put(Action.DELETE, new KeyCodeCombination(KeyCode.DELETE));
        map.put(Action.UPLOAD, new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
        map.put(Action.DOWNLOAD, new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
        map.put(Action.FOCUS_PATH, new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        map.put(Action.OPEN_DIR, new KeyCodeCombination(KeyCode.ENTER));
        map.put(Action.LOGOUT, new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        map.put(Action.NEW_CONNECTION, new KeyCodeCombination(KeyCode.N, KeyCombination.ALT_DOWN));
    }

    public void register(Scene scene, Map<Action, Runnable> handlers) {
        // clear & set fresh
        scene.getAccelerators().clear();
        for (var e : map.entrySet()) {
            Runnable r = handlers.get(e.getKey());
            if (r != null) scene.getAccelerators().put(e.getValue(), r);
        }
    }

    public void showDialog(Stage owner, Consumer<Void> onChanged) {
        Stage dlg = new Stage();
        dlg.setTitle("Shortcuts");
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);

        TableView<Row> table = new TableView<>();
        TableColumn<Row, String> cAction = new TableColumn<>("Aktion");
        cAction.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().action.name()));
        cAction.setPrefWidth(220);

        TableColumn<Row, String> cKey = new TableColumn<>("Shortcut");
        cKey.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().combo.getDisplayText()));
        cKey.setPrefWidth(200);

        TableColumn<Row, Void> cEdit = new TableColumn<>("Ändern");
        cEdit.setCellFactory(col -> new TableCell<>() {
            final Button b = new Button("Setzen");
            { b.setOnAction(e -> captureShortcut(getTableView().getItems().get(getIndex()), table)); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : b);
            }
        });

        table.getColumns().setAll(cAction, cKey, cEdit);
        for (var e : map.entrySet()) table.getItems().add(new Row(e.getKey(), e.getValue()));

        Button btnReset = new Button("Standard");
        btnReset.setOnAction(e -> { defaults(); refresh(table); save(); if (onChanged!=null) onChanged.accept(null); });

        Button btnClose = new Button("Schließen");
        btnClose.setOnAction(e -> { save(); if (onChanged!=null) onChanged.accept(null); dlg.close(); });

        HBox buttons = new HBox(10, btnReset, new Pane(), btnClose);
        HBox.setHgrow(buttons.getChildren().get(1), Priority.ALWAYS);

        VBox root = new VBox(10, table, buttons);
        root.setPadding(new Insets(12));
        dlg.setScene(new Scene(root, 520, 420));
        dlg.show();
    }

    private void captureShortcut(Row row, TableView<Row> table) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Drücke neue Tastenkombi…", ButtonType.CANCEL);
        a.setHeaderText("Shortcut für " + row.action.name());

        a.getDialogPane().getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            var code = ev.getCode();
            if (code == javafx.scene.input.KeyCode.UNDEFINED) return;

            // 👉 NUR gesetzte Modifier sammeln (keine NO_MATCH-Werte!)
            java.util.List<javafx.scene.input.KeyCombination.Modifier> mods = new java.util.ArrayList<>();
            if (ev.isControlDown()) mods.add(javafx.scene.input.KeyCombination.CONTROL_DOWN);
            if (ev.isAltDown())     mods.add(javafx.scene.input.KeyCombination.ALT_DOWN);
            if (ev.isShiftDown())   mods.add(javafx.scene.input.KeyCombination.SHIFT_DOWN);
            if (ev.isMetaDown())    mods.add(javafx.scene.input.KeyCombination.META_DOWN);

            javafx.scene.input.KeyCombination kc =
                    new javafx.scene.input.KeyCodeCombination(code, mods.toArray(new javafx.scene.input.KeyCombination.Modifier[0]));

            // speichern + UI refreshen
            map.put(row.action, kc);
            refresh(table);
            save();

            a.close();
            ev.consume();
        });

        a.showAndWait();
    }

    private void refresh(TableView<Row> table) {
        table.getItems().clear();
        for (var e : map.entrySet()) table.getItems().add(new Row(e.getKey(), e.getValue()));
    }

    private record Row(Action action, KeyCombination combo) {}

    private void load() {
        try {
            if (!file.isFile()) return;
            var type = new TypeReference<Map<String,String>>() {};
            Map<String,String> raw = new ObjectMapper().readValue(file, type);
            for (var e : raw.entrySet()) {
                Action a = Action.valueOf(e.getKey());
                KeyCombination kc = KeyCombination.valueOf(e.getValue());
                map.put(a, kc);
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            var dir = file.getParentFile();
            if (!dir.isDirectory()) dir.mkdirs();
            Map<String,String> raw = new LinkedHashMap<>();
            for (var e : map.entrySet()) raw.put(e.getKey().name(), e.getValue().getName());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, raw);
        } catch (Exception ignored) {}
    }
}
