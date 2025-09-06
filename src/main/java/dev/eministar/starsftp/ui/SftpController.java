package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.AppNavigator;
import dev.eministar.starsftp.model.Profile;
import dev.eministar.starsftp.sftp.SftpService;
import dev.eministar.starsftp.sftp.SftpService.RemoteItem;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.function.Consumer;

public class SftpController {

    @FXML private Label lblConn;
    @FXML private TextField tfRemotePath;
    @FXML private Button btnGo, btnUp, btnRoot, btnHome, btnRefresh, btnUpload, btnDownload, btnLogout, btnMkdir, btnRename, btnDelete;
    @FXML private Button btnShortcuts, btnNewConn;
    @FXML private ProgressIndicator pi;
    @FXML private TableView<RemoteItem> tblRemote;
    @FXML private HBox bcBar;

    private SftpService sftp;
    private Profile profile;
    private String homeDir = "/";
    private String currentRemotePath = homeDir;

    private final ShortcutPrefs shortcuts = new ShortcutPrefs();

    public void init(SftpService sftp, Profile profile) {
        this.sftp = sftp;
        this.profile = profile;

        lblConn.setText("  •  " + profile.username() + "@" + profile.host() + ":" + profile.port());

        // Icons
        btnUpload.setGraphic(new FontIcon(Feather.UPLOAD));
        btnDownload.setGraphic(new FontIcon(Feather.DOWNLOAD));
        btnLogout.setGraphic(new FontIcon(Feather.LOG_OUT));
        btnRefresh.setGraphic(new FontIcon(Feather.REFRESH_CW));
        btnUp.setGraphic(new FontIcon(Feather.CHEVRON_UP));
        btnGo.setGraphic(new FontIcon(Feather.CORNER_DOWN_RIGHT));
        btnMkdir.setGraphic(new FontIcon(Feather.FOLDER_PLUS));
        btnRename.setGraphic(new FontIcon(Feather.EDIT_2));
        btnDelete.setGraphic(new FontIcon(Feather.TRASH_2));
        btnShortcuts.setGraphic(new FontIcon(Feather.KEY));
        btnNewConn.setGraphic(new FontIcon(Feather.PLUS_SQUARE));

        setupRemoteTable();
        setupTableDnD();
        Platform.runLater(this::setupShortcuts);

        // initial
        runAsync("Liste laden", () -> {
            var items = sftp.list(null);
            homeDir = sftp.getCurrentDir();
            return items;
        }, items -> {
            tfRemotePath.setText(sftp.getCurrentDir());
            updateBreadcrumbs();
            fillRemote(items);
        });
    }

    private void setupShortcuts() {
        var scene = tblRemote.getScene();
        if (scene == null) return;
        Map<ShortcutPrefs.Action, Runnable> map = new EnumMap<>(ShortcutPrefs.Action.class);
        map.put(ShortcutPrefs.Action.REFRESH, this::onRefresh);
        map.put(ShortcutPrefs.Action.UP, this::onUp);
        map.put(ShortcutPrefs.Action.GO, this::onGo);
        map.put(ShortcutPrefs.Action.ROOT, this::onRoot);
        map.put(ShortcutPrefs.Action.HOME, this::onHome);
        map.put(ShortcutPrefs.Action.MKDIR, this::onMkdir);
        map.put(ShortcutPrefs.Action.RENAME, this::onRename);
        map.put(ShortcutPrefs.Action.DELETE, this::onDelete);
        map.put(ShortcutPrefs.Action.UPLOAD, this::onUpload);
        map.put(ShortcutPrefs.Action.DOWNLOAD, this::onDownload);
        map.put(ShortcutPrefs.Action.FOCUS_PATH, () -> { tfRemotePath.requestFocus(); tfRemotePath.selectAll(); });
        map.put(ShortcutPrefs.Action.OPEN_DIR, () -> {
            RemoteItem it = tblRemote.getSelectionModel().getSelectedItem();
            if (it != null && it.directory()) navigateTo(it.path());
        });
        map.put(ShortcutPrefs.Action.LOGOUT, this::onLogout);
        map.put(ShortcutPrefs.Action.NEW_CONNECTION, this::onNewConnection);
        shortcuts.register(scene, map);
    }

    private void setupRemoteTable() {
        // Multi-select
        tblRemote.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<RemoteItem, Void> cIco = new TableColumn<>();
        cIco.setPrefWidth(36);
        cIco.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                RemoteItem it = getTableView().getItems().get(getIndex());
                setGraphic(new FontIcon(it.directory() ? Feather.FOLDER : Feather.FILE));
            }
        });

        TableColumn<RemoteItem, String> cName = new TableColumn<>("Name");
        cName.setPrefWidth(380);
        cName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name()));

        TableColumn<RemoteItem, String> cType = new TableColumn<>("Typ");
        cType.setPrefWidth(90);
        cType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().directory() ? "Ordner" : "Datei"));

        TableColumn<RemoteItem, String> cSize = new TableColumn<>("Größe");
        cSize.setPrefWidth(120);
        cSize.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().directory() ? "-" : human(d.getValue().size())
        ));

        TableColumn<RemoteItem, String> cMod = new TableColumn<>("Geändert");
        cMod.setPrefWidth(200);
        cMod.setCellValueFactory(d -> {
            long ts = d.getValue().modifiedEpochMs();
            String s = ts <= 0 ? "-" : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(ts));
            return new javafx.beans.property.SimpleStringProperty(s);
        });

        tblRemote.getColumns().setAll(cIco, cName, cType, cSize, cMod);

        // Double-Click → Ordner öffnen
        tblRemote.setRowFactory(tv -> {
            TableRow<RemoteItem> row = new TableRow<>();

            // Kontextmenü
            ContextMenu cm = buildContextMenu();
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(cm));

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    RemoteItem it = row.getItem();
                    if (it.directory()) navigateTo(it.path());
                }
            });

            // DnD intern (move)
            row.setOnDragDetected(e -> {
                if (row.isEmpty()) return;
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(row.getItem().path());
                db.setContent(cc);
                e.consume();
            });
            row.setOnDragOver(e -> {
                if (row.isEmpty()) return;
                RemoteItem target = row.getItem();
                if (target.directory()) {
                    Dragboard db = e.getDragboard();
                    if (db.hasString() && !db.getString().equals(target.path())) {
                        e.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                e.consume();
            });
            row.setOnDragDropped(e -> {
                boolean success = false;
                try {
                    if (!row.isEmpty() && row.getItem().directory()) {
                        String src = e.getDragboard().getString();
                        RemoteItem target = row.getItem();
                        String base = target.path().endsWith("/") ? target.path() : target.path() + "/";
                        String name = src.substring(src.lastIndexOf('/') + 1);
                        String dest = base + name;
                        runAsync("Verschieben", () -> { sftp.rename(src, dest); return null; },
                                v -> { toast("Verschoben → " + target.name()); onRefresh(); });
                        success = true;
                    }
                } finally {
                    e.setDropCompleted(success);
                    e.consume();
                }
            });

            return row;
        });
    }

    private ContextMenu buildContextMenu() {
        var cm = new ContextMenu();
        var miOpen = new MenuItem("Öffnen"); miOpen.setOnAction(e -> {
            RemoteItem it = tblRemote.getSelectionModel().getSelectedItem();
            if (it != null && it.directory()) navigateTo(it.path());
        });
        var miProps = new MenuItem("Eigenschaften…");
        miProps.setOnAction(e -> openPropsForSelection());
        var miDownload = new MenuItem("Download"); miDownload.setOnAction(e -> onDownload());
        var miRename = new MenuItem("Umbenennen"); miRename.setOnAction(e -> onRename());
        var miDelete = new MenuItem("Löschen"); miDelete.setOnAction(e -> onDelete());
        var miNew = new MenuItem("Neuer Ordner…"); miNew.setOnAction(e -> onMkdir());
        cm.getItems().addAll(
                miOpen,
                miProps,
                new SeparatorMenuItem(),
                miDownload, miRename, miDelete,
                new SeparatorMenuItem(),
                miNew
        );
        return cm;
    }

    private void openPropsForSelection() {
        var sel = tblRemote.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String abs = sel.path(); // <-- RemoteItem liefert already den absoluten Pfad

        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/props-dialog.fxml"));
            DialogPane pane = loader.load();
            pane.getStyleClass().add("themed-dialog");


            Button ok = (Button) pane.lookupButton(ButtonType.OK);
            if (ok != null) ok.getStyleClass().add("primary");

            Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
            if (cancel != null) cancel.getStyleClass().add("danger");

            PropsController c = loader.getController();
            c.init(sftp, abs, sel.name());

            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setDialogPane(pane);
            dlg.setTitle("Eigenschaften – " + sel.name());
            dlg.initOwner(tblRemote.getScene().getWindow());

            var res = dlg.showAndWait();
            if (res.isPresent() && res.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                c.save();
                onRefresh();
            }
        } catch (Exception ex) {
            toast("Eigenschaften fehlgeschlagen: " + ex.getMessage());
        }
    }


    private void setupTableDnD() {
        // OS → Upload
        tblRemote.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        tblRemote.setOnDragDropped(e -> {
            boolean success = false;
            try {
                Dragboard db = e.getDragboard();
                if (db.hasFiles()) {
                    String targetDir = sftp.getCurrentDir();
                    var files = db.getFiles();
                    runAsync("Upload", () -> { for (File f : files) sftp.upload(f, targetDir); return null; },
                            v -> { toast("Upload ok (" + files.size() + ")"); onRefresh(); });
                    success = true;
                }
            } finally {
                e.setDropCompleted(success);
                e.consume();
            }
        });

        // Backspace navigiert hoch
        tblRemote.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.BACK_SPACE) onUp();
        });
    }

    private void fillRemote(List<RemoteItem> items) {
        items.sort(Comparator
                .comparing(RemoteItem::directory).reversed()
                .thenComparing(RemoteItem::name, String.CASE_INSENSITIVE_ORDER));
        tblRemote.getItems().setAll(items);
    }

    // ------------ Buttons & Actions ------------
    @FXML public void onRefresh() { navigateTo(sanitize(tfRemotePath.getText())); }
    @FXML public void onGo() { navigateTo(sanitize(tfRemotePath.getText())); }
    @FXML public void onUp() {
        String cur = sftp.getCurrentDir();
        if ("/".equals(cur)) { toast("Schon im Root"); return; }
        int i = cur.lastIndexOf('/');
        String parent = (i <= 0) ? "/" : cur.substring(0, i);
        navigateTo(parent);
    }
    @FXML public void onRoot() { navigateTo("/"); }
    @FXML public void onHome() { navigateTo(homeDir); }
    @FXML public void onNewConnection() { AppNavigator.showLogin(); }

    @FXML public void onUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Dateien hochladen");
        List<File> files = fc.showOpenMultipleDialog(tblRemote.getScene().getWindow());
        if (files == null || files.isEmpty()) return;
        final String targetDir = sftp.getCurrentDir();
        runAsync("Upload", () -> { for (File f : files) sftp.upload(f, targetDir); return null; },
                v -> { toast("Upload ok (" + files.size() + ")"); onRefresh(); });
    }

    @FXML public void onDownload() {
        var items = tblRemote.getSelectionModel().getSelectedItems();
        if (items == null || items.isEmpty()) { toast("Wähle Datei(en)"); return; }
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Zielordner wählen");
        File dir = dc.showDialog(tblRemote.getScene().getWindow());
        if (dir == null) return;
        runAsync("Download", () -> { for (var it : items) if (!it.directory()) sftp.download(it.path(), dir); return null; },
                v -> toast("Download ok → " + dir.getAbsolutePath()));
    }

    @FXML public void onMkdir() {
        TextInputDialog dlg = new TextInputDialog("NeuerOrdner");
        dlg.setHeaderText("Neuen Ordner anlegen"); dlg.setContentText("Name:");
        dlg.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            String base = sftp.getCurrentDir();
            String dir = base.endsWith("/") ? base + name : base + "/" + name;
            runAsync("mkdir", () -> { sftp.mkdir(dir); return null; },
                    v -> { toast("Ordner erstellt"); onRefresh(); });
        });
    }

    @FXML public void onRename() {
        var it = tblRemote.getSelectionModel().getSelectedItem();
        if (it == null) { toast("Wähle Eintrag"); return; }
        TextInputDialog dlg = new TextInputDialog(it.name());
        dlg.setHeaderText("Umbenennen"); dlg.setContentText("Neuer Name:");
        dlg.showAndWait().ifPresent(nn -> {
            if (nn.isBlank()) return;
            String base = sftp.getCurrentDir();
            String target = base.endsWith("/") ? base + nn : base + "/" + nn;
            runAsync("rename", () -> { sftp.rename(it.path(), target); return null; },
                    v -> { toast("Umbenannt"); onRefresh(); });
        });
    }

    @FXML public void onDelete() {
        var items = new ArrayList<>(tblRemote.getSelectionModel().getSelectedItems());
        if (items.isEmpty()) { toast("Wähle Einträge"); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Wirklich löschen (" + items.size() + ")?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Löschen");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        runAsync("delete", () -> { for (var it : items) sftp.delete(it.path(), true); return null; },
                v -> { toast("Gelöscht"); onRefresh(); });
    }

    @FXML public void onLogout() {
        runAsync("Logout", () -> { sftp.close(); return null; }, v -> AppNavigator.showLogin());
    }

    @FXML public void onOpenShortcutDialog() {
        shortcuts.showDialog((javafx.stage.Stage) tblRemote.getScene().getWindow(), _v -> setupShortcuts());
    }

    // ------------ Helpers ------------
    private void navigateTo(String requestedPath) {
        String req = sanitize(requestedPath);
        runAsync("Wechsel", () -> sftp.list(req), items -> {
            tfRemotePath.setText(sftp.getCurrentDir());
            updateBreadcrumbs();
            fillRemote(items);
        });
    }

    private void updateBreadcrumbs() {
        bcBar.getChildren().clear();
        String cur = sftp.getCurrentDir();

        Button root = makeCrumb("/", "/");
        bcBar.getChildren().add(root);

        if ("/".equals(cur)) return;

        String path = "";
        for (String seg : cur.split("/")) {
            if (seg.isBlank()) continue;
            path = path + "/" + seg;
            bcBar.getChildren().add(new Label("›"));
            bcBar.getChildren().add(makeCrumb(seg, path));
        }
    }

    private Button makeCrumb(String text, String target) {
        Button b = new Button(text);
        b.getStyleClass().add("link-like");
        b.setPadding(new Insets(2, 6, 2, 6));
        b.setOnAction(e -> navigateTo(target));
        return b;
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return sftp.getCurrentDir();
        String s = raw.trim();
        if ("~".equals(s)) return homeDir;
        if (!s.startsWith("/") && !s.startsWith("~")) {
            String cur = sftp.getCurrentDir();
            s = cur.endsWith("/") ? cur + s : cur + "/" + s;
        }
        while (s.contains("//")) s = s.replace("//", "/");
        return s;
    }

    private <T> void runAsync(String label, TaskCallable<T> work, Consumer<T> onOk) {
        setUiDisabled(true);
        pi.setVisible(true);
        Task<T> t = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        t.setOnSucceeded(e -> { pi.setVisible(false); setUiDisabled(false); if (onOk != null) onOk.accept(t.getValue()); });
        t.setOnFailed(e -> { pi.setVisible(false); setUiDisabled(false); toast(label + " fehlgeschlagen: " + t.getException().getMessage()); });
        new Thread(t, "sftp-task").start();
    }

    private void setUiDisabled(boolean b) {
        btnGo.setDisable(b); btnUp.setDisable(b); btnRoot.setDisable(b); btnHome.setDisable(b);
        btnRefresh.setDisable(b); btnUpload.setDisable(b); btnDownload.setDisable(b);
        btnMkdir.setDisable(b); btnRename.setDisable(b); btnDelete.setDisable(b);
        btnLogout.setDisable(b); btnShortcuts.setDisable(b); btnNewConn.setDisable(b);
        tfRemotePath.setDisable(b); tblRemote.setDisable(b);
    }

    private void toast(String msg) { Notifications.create().position(javafx.geometry.Pos.TOP_RIGHT).text(msg).showInformation(); }
    @FunctionalInterface private interface TaskCallable<T> { T call() throws Exception; }

    private static String human(long bytes) {
        if (bytes < 0) return "-";
        if (bytes < 1024) return bytes + " B";
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        double v = bytes;
        int i = 0;
        while (v >= 1024 && i < 6) { v /= 1024; ci.next(); i++; }
        return String.format(java.util.Locale.ROOT, "%.1f %cB", v, ci.current());
    }
}
