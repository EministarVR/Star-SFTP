package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.AppNavigator;
import dev.eministar.starsftp.model.Profile;
import dev.eministar.starsftp.sftp.SftpService;
import dev.eministar.starsftp.store.ProfileStore;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.stream.Collectors;

public class LoginController {
    @FXML private TextField tfSearch;
    @FXML private TilePane tile;

    // Quick connect
    @FXML private TextField qcHost, qcUser;
    @FXML private PasswordField qcPass;
    @FXML private Button btnQuickConnect, btnNew;

    // Overlay
    @FXML private StackPane overlay;
    @FXML private Label lblConnecting, lblConnectingSub;
    @FXML private Button btnCancelConnect;

    private final ProfileStore store = new ProfileStore();
    private final SftpService sftp = new SftpService();

    // laufender Connect-Task (zum Canceln)
    private Task<Void> currentTask;

    @FXML
    public void initialize() {
        // Events
        tfSearch.textProperty().addListener((o, ov, nv) -> refreshGrid());
        btnNew.setOnAction(e -> onCreate());
        btnQuickConnect.setOnAction(e -> onQuickConnect());
        btnCancelConnect.setOnAction(e -> cancelConnect());

        // Enter in Quick-Fields triggert Connect
        qcHost.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onQuickConnect(); });
        qcUser.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onQuickConnect(); });
        qcPass.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onQuickConnect(); });

        refreshGrid();
    }

    /* ===================== GRID / KARTEN ===================== */

    private void refreshGrid() {
        tile.getChildren().clear();
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        var profiles = new ArrayList<>(store.profiles().values());
        if (!q.isBlank()) {
            profiles = profiles.stream().filter(p ->
                    (p.name() != null && p.name().toLowerCase().contains(q)) ||
                            (p.host() != null && p.host().toLowerCase().contains(q)) ||
                            (p.username() != null && p.username().toLowerCase().contains(q))
            ).collect(Collectors.toCollection(ArrayList::new));
        }

        // “Neu”-Karte zuerst
        tile.getChildren().add(makeNewCard());

        // Profile-Karten
        for (Profile p : profiles) {
            tile.getChildren().add(makeHostCard(p));
        }
    }

    private Node makeNewCard() {
        var card = baseCard();
        var icon = new FontIcon(Feather.PLUS_CIRCLE);
        icon.setIconSize(28);

        var title = new Label("Neuer Host");
        title.getStyleClass().add("card-title");

        var sub = new Label("Profil anlegen");
        sub.getStyleClass().add("card-sub");

        VBox box = new VBox(8, icon, title, sub);
        box.setAlignment(Pos.CENTER_LEFT);

        card.setOnMouseClicked(e -> onCreate());
        card.getChildren().add(box);
        return card;
    }

    private Node makeHostCard(Profile p) {
        var card = baseCard();

        // Header mit Name und Aktionen
        var name = new Label(p.name());
        name.getStyleClass().add("card-title");

        var btnConnect = smallIconButton(Feather.PLAY);
        btnConnect.setTooltip(new Tooltip("Verbinden"));
        btnConnect.setOnAction(e -> connect(p));

        var btnEdit = smallIconButton(Feather.EDIT_2);
        btnEdit.setTooltip(new Tooltip("Bearbeiten"));
        btnEdit.setOnAction(e -> onEdit(p));

        var btnDelete = smallIconButton(Feather.TRASH_2);
        btnDelete.getStyleClass().add("danger");
        btnDelete.setTooltip(new Tooltip("Löschen"));
        btnDelete.setOnAction(e -> {
            if (Dialogs.confirm(tile.getScene().getWindow(),
                    "Löschen", "Profil \"" + p.name() + "\" wirklich löschen?",
                    LoginController.class)) {
                store.delete(p.name());
                refreshGrid();
                toast("Profil gelöscht");
            }
        });

        HBox actions = new HBox(8, btnConnect, btnEdit, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(name, new Region(), actions);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        // Body
        var line1 = new Label("%s@%s:%d".formatted(
                nn(p.username()), nn(p.host()), p.port()));
        line1.getStyleClass().add("card-line");

        var line2 = new Label(p.password() != null && !p.password().isBlank()
                ? "Auth: Passwort • gespeichert: " + (p.remember() ? "Ja" : "Nein")
                : "Auth: Key/Passwort extern • gespeichert: " + (p.remember() ? "Ja" : "Nein"));
        line2.getStyleClass().add("card-meta");

        VBox body = new VBox(6, line1, line2);

        VBox box = new VBox(10, header, body);
        box.setAlignment(Pos.TOP_LEFT);

        // gesamtes Card-Klicken => connect
        card.setOnMouseClicked(e -> connect(p));

        // Kontextmenü
        ContextMenu cm = new ContextMenu();
        var miConnect = new MenuItem("Verbinden"); miConnect.setOnAction(e -> connect(p));
        var miEdit = new MenuItem("Bearbeiten…"); miEdit.setOnAction(e -> onEdit(p));
        var miDelete = new MenuItem("Löschen"); miDelete.setOnAction(e -> {
            if (Dialogs.confirm(tile.getScene().getWindow(),
                    "Löschen", "Profil \"" + p.name() + "\" wirklich löschen?",
                    LoginController.class)) {
                store.delete(p.name()); refreshGrid(); toast("Profil gelöscht");
            }
        });
        cm.getItems().addAll(miConnect, miEdit, new SeparatorMenuItem(), miDelete);
        card.setOnContextMenuRequested(e -> cm.show(card, e.getScreenX(), e.getScreenY()));

        card.getChildren().add(box);
        return card;
    }

    private StackPane baseCard() {
        var card = new StackPane();
        card.getStyleClass().add("host-card");
        card.setPadding(new Insets(14));
        card.setPrefSize(240, 140);
        return card;
    }

    private Button smallIconButton(Feather icon) {
        var b = new Button();
        b.setGraphic(new FontIcon(icon));
        b.getStyleClass().add("icon-btn");
        return b;
    }

    private static String nn(String s) { return s == null ? "" : s; }

    /* ===================== CREATE/EDIT ===================== */

    private void onCreate() {
        HostDialogs.showCreateOrEdit(null, LoginController.class).ifPresent(p -> {
            store.save(p);
            refreshGrid();
            toast("Profil angelegt");
        });
    }

    private void onEdit(Profile edit) {
        HostDialogs.showCreateOrEdit(edit, LoginController.class).ifPresent(p -> {
            store.save(p);
            refreshGrid();
            toast("Profil aktualisiert");
        });
    }

    /* ===================== CONNECT ===================== */

    private void connect(Profile p) {
        showOverlay(true, "Verbinde…", "%s@%s:%d".formatted(p.username(), p.host(), p.port()));
        currentTask = new Task<>() {
            @Override protected Void call() throws Exception {
                sftp.connect(p);
                return null;
            }
        };
        currentTask.setOnSucceeded(e -> {
            showOverlay(false, null, null);
            AppNavigator.showSftp(sftp, p);
        });
        currentTask.setOnFailed(e -> {
            showOverlay(false, null, null);
            String msg = Optional.ofNullable(currentTask.getException())
                    .map(Throwable::getMessage).orElse("?");
            toast("Verbindung fehlgeschlagen: " + msg);
        });
        new Thread(currentTask, "connect").start();
    }

    private void onQuickConnect() {
        String host = qcHost.getText().trim();
        if (host.isEmpty()) { toast("Host fehlt"); return; }
        String user = qcUser.getText().trim();
        if (user.isEmpty()) { toast("User fehlt"); return; }

        int port = 22;
        String h = host;
        int idx = host.lastIndexOf(':');
        if (idx > 0) {
            try { port = Integer.parseInt(host.substring(idx+1)); h = host.substring(0, idx); }
            catch (Exception ignored) {}
        }

        Profile tmp = new Profile("Quick: " + h, h, port, user,
                qcPass.getText().isBlank() ? null : qcPass.getText(),
                null, false);

        connect(tmp);
    }

    private void cancelConnect() {
        if (currentTask != null && currentTask.isRunning()) currentTask.cancel(true);
        showOverlay(false, null, null);
    }

    /* ===================== OVERLAY/ANIMATION ===================== */

    private void showOverlay(boolean on, String title, String sub) {
        if (title != null) lblConnecting.setText(title);
        if (sub != null) lblConnectingSub.setText(sub);

        overlay.setVisible(true);
        FadeTransition f = new FadeTransition(Duration.millis(180), overlay);
        f.setFromValue(on ? 0 : 1);
        f.setToValue(on ? 1 : 0);

        Node card = overlay.lookup(".overlay-card");
        if (card != null) {
            ScaleTransition s = new ScaleTransition(Duration.millis(180), card);
            s.setFromX(on ? 0.98 : 1.0); s.setFromY(on ? 0.98 : 1.0);
            s.setToX(on ? 1.0 : 0.98);   s.setToY(on ? 1.0 : 0.98);
            s.play();
        }

        f.setOnFinished(e -> { if (!on) overlay.setVisible(false); });
        if (on) { overlay.setOpacity(0); }
        f.play();
    }

    /* ===================== Utils ===================== */

    private void toast(String msg) {
        Notifications.create().position(Pos.TOP_RIGHT).text(msg).showInformation();
    }
}
