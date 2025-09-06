package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.AppNavigator;
import dev.eministar.starsftp.model.Profile;
import dev.eministar.starsftp.sftp.SftpService;
import dev.eministar.starsftp.store.ProfileStore;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.Optional;

public class LoginController {

    @FXML private ComboBox<String> cbProfiles;
    @FXML private TextField tfName, tfHost, tfPort, tfUser, tfKey;
    @FXML private PasswordField pfPass;
    @FXML private CheckBox chkRemember;
    @FXML private Button btnSave, btnConnect, btnDelete, btnPickKey;
    @FXML private ProgressIndicator pi;

    private final ProfileStore store = new ProfileStore();

    @FXML
    public void initialize() {
        cbProfiles.getItems().setAll(store.profiles().keySet());
        store.lastProfile().ifPresent(n -> cbProfiles.getSelectionModel().select(n));
        cbProfiles.valueProperty().addListener((o, ov, nv) -> load(nv));
        store.lastProfile().ifPresent(this::load);
    }

    private void load(String name) {
        if (name == null) return;
        Profile p = store.profiles().get(name);
        if (p == null) return;
        tfName.setText(p.name());
        tfHost.setText(p.host());
        tfPort.setText(String.valueOf(p.port()));
        tfUser.setText(p.username());
        pfPass.setText(p.password() == null ? "" : p.password());
        tfKey.setText(p.privateKeyPath() == null ? "" : p.privateKeyPath());
        chkRemember.setSelected(p.remember());
    }

    @FXML public void onPickKey() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Private Key auswählen");
        File f = fc.showOpenDialog(btnPickKey.getScene().getWindow());
        if (f != null) tfKey.setText(f.getAbsolutePath());
    }

    @FXML public void onSave() {
        Optional<Profile> prof = buildProfile();
        if (prof.isEmpty()) return;
        Profile p = prof.get();
        store.save(p);
        if (!cbProfiles.getItems().contains(p.name())) cbProfiles.getItems().add(p.name());
        cbProfiles.getSelectionModel().select(p.name());
        store.setLastProfile(p.name());
        toast("Profil gespeichert");
    }

    @FXML public void onDelete() {
        String n = cbProfiles.getValue();
        if (n == null) return;
        if (!confirm("Profil löschen", "Profil \"" + n + "\" wirklich löschen?")) return;
        store.delete(n);
        cbProfiles.getItems().setAll(store.profiles().keySet());
        toast("Profil gelöscht");
    }

    @FXML public void onConnect() {
        Optional<Profile> prof = buildProfile();
        if (prof.isEmpty()) return;
        Profile p = prof.get();
        store.setLastProfile(p.name());
        disableUi(true);
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                SftpService sftp = new SftpService();
                sftp.connect(p);
                // Erfolg: ins SFTP-View
                javafx.application.Platform.runLater(() -> AppNavigator.showSftp(sftp, p));
                return null;
            }
        };
        t.setOnSucceeded(e -> disableUi(false));
        t.setOnFailed(e -> { disableUi(false); toast("Login fehlgeschlagen: " + t.getException().getMessage()); });
        new Thread(t, "login").start();
    }

    private Optional<Profile> buildProfile() {
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (Exception e) {
            toast("Port ungültig");
            return Optional.empty();
        }
        if (blank(tfName) || blank(tfHost) || blank(tfUser)) { toast("Name/Host/User dürfen nicht leer sein"); return Optional.empty(); }
        return Optional.of(new Profile(
                tfName.getText().trim(),
                tfHost.getText().trim(),
                port,
                tfUser.getText().trim(),
                pfPass.getText().isBlank() ? null : pfPass.getText(),
                tfKey.getText().isBlank() ? null : tfKey.getText(),
                chkRemember.isSelected()
        ));
    }

    private boolean blank(TextField tf) { return tf.getText() == null || tf.getText().isBlank(); }
    private void disableUi(boolean b) { btnConnect.setDisable(b); btnSave.setDisable(b); btnDelete.setDisable(b); cbProfiles.setDisable(b); pi.setVisible(b); }
    private void toast(String msg) { Notifications.create().text(msg).showInformation(); }
    private boolean confirm(String h, String m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, m, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(h); return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
