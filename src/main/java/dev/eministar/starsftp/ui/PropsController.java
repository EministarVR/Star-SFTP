package dev.eministar.starsftp.ui;

import dev.eministar.starsftp.sftp.SftpService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;

public class PropsController {
    @FXML private TextField tfName, tfPath, tfOwner, tfGroup, tfOctal;
    @FXML private CheckBox ownR, ownW, ownX, grpR, grpW, grpX, othR, othW, othX;
    @FXML private CheckBox setUid, setGid, sticky;
    @FXML private Label lblInfo;

    private SftpService sftp;
    private String absPath;
    private String baseName;
    private FileAttributes original;

    public void init(SftpService sftp, String absPath, String baseName) throws Exception {
        this.sftp = sftp;
        this.absPath = absPath;
        this.baseName = baseName;

        tfName.setText(baseName);
        tfPath.setText(absPath);

        original = sftp.stat(absPath);
        fillFromAttrs(original);

        tfOctal.textProperty().addListener((o, ov, nv) -> {
            if (nv != null) try { applyOctalToChecks(nv.trim()); } catch (Exception ignored) {}
        });
        var sync = (javafx.beans.value.ChangeListener<Boolean>) (o, ov, nv) -> tfOctal.setText(toOctalString(currentMode()));
        ownR.selectedProperty().addListener(sync); ownW.selectedProperty().addListener(sync); ownX.selectedProperty().addListener(sync);
        grpR.selectedProperty().addListener(sync); grpW.selectedProperty().addListener(sync); grpX.selectedProperty().addListener(sync);
        othR.selectedProperty().addListener(sync); othW.selectedProperty().addListener(sync); othX.selectedProperty().addListener(sync);
        setUid.selectedProperty().addListener((o,ov,nv)-> tfOctal.setText(toOctalString(currentMode())));
        setGid.selectedProperty().addListener((o,ov,nv)-> tfOctal.setText(toOctalString(currentMode())));
        sticky.selectedProperty().addListener((o,ov,nv)-> tfOctal.setText(toOctalString(currentMode())));

        var type = original.getMode().getType();
        lblInfo.setText("Größe: " + original.getSize() + " • Typ: " + (type == FileMode.Type.DIRECTORY ? "Ordner" : "Datei"));
    }

    private void fillFromAttrs(FileAttributes a) {
        // UID/GID optional – API kann je nach Version primitive ints liefern.
        // Wir zeigen sie nur an, wenn >0 (primitive) bzw. ignorieren Fehler.
        try {
            int uid = (int) FileAttributes.class.getMethod("getUID").invoke(a);
            if (uid > 0) tfOwner.setText(String.valueOf(uid));
        } catch (Exception ignored) {}
        try {
            int gid = (int) FileAttributes.class.getMethod("getGID").invoke(a);
            if (gid > 0) tfGroup.setText(String.valueOf(gid));
        } catch (Exception ignored) {}

        int m = a.getMode().getPermissionsMask();

        setUid.setSelected((m & 04000) != 0);
        setGid.setSelected((m & 02000) != 0);
        sticky.setSelected((m & 01000) != 0);

        ownR.setSelected((m & 0400) != 0);
        ownW.setSelected((m & 0200) != 0);
        ownX.setSelected((m & 0100) != 0);
        grpR.setSelected((m & 0040) != 0);
        grpW.setSelected((m & 0020) != 0);
        grpX.setSelected((m & 0010) != 0);
        othR.setSelected((m & 0004) != 0);
        othW.setSelected((m & 0002) != 0);
        othX.setSelected((m & 0001) != 0);

        tfOctal.setText(toOctalString(m));
    }

    private int currentMode() {
        int m = 0;
        if (ownR.isSelected()) m |= 0400;
        if (ownW.isSelected()) m |= 0200;
        if (ownX.isSelected()) m |= 0100;
        if (grpR.isSelected()) m |= 0040;
        if (grpW.isSelected()) m |= 0020;
        if (grpX.isSelected()) m |= 0010;
        if (othR.isSelected()) m |= 0004;
        if (othW.isSelected()) m |= 0002;
        if (othX.isSelected()) m |= 0001;
        if (setUid.isSelected()) m |= 04000;
        if (setGid.isSelected()) m |= 02000;
        if (sticky.isSelected()) m |= 01000;
        return m;
    }

    private void applyOctalToChecks(String oct) {
        if (oct.isEmpty()) return;
        String s = oct.startsWith("0") ? oct.substring(1) : oct;
        if (!(s.length() == 3 || s.length() == 4)) return;
        int m = Integer.parseInt(s, 8);

        setUid.setSelected((m & 04000) != 0);
        setGid.setSelected((m & 02000) != 0);
        sticky.setSelected((m & 01000) != 0);
        ownR.setSelected((m & 0400) != 0);
        ownW.setSelected((m & 0200) != 0);
        ownX.setSelected((m & 0100) != 0);
        grpR.setSelected((m & 0040) != 0);
        grpW.setSelected((m & 0020) != 0);
        grpX.setSelected((m & 0010) != 0);
        othR.setSelected((m & 0004) != 0);
        othW.setSelected((m & 0002) != 0);
        othX.setSelected((m & 0001) != 0);

        tfOctal.setText(toOctalString(m));
    }

    private String toOctalString(int m) {
        String s = Integer.toOctalString(m);
        if (s.length() < 4) s = ("0000" + s).substring(s.length());
        return "0" + s;
    }

    /** Nach OK drücken callen */
    public void save() throws Exception {
        int mode = currentMode();
        sftp.chmod(absPath, mode);  // ← einfacher & versionssicher

        try {
            if (!tfOwner.getText().isBlank()) {
                int uid = Integer.parseInt(tfOwner.getText().trim());
                sftp.chown(absPath, uid);
            }
        } catch (NumberFormatException ignored) {}

        try {
            if (!tfGroup.getText().isBlank()) {
                int gid = Integer.parseInt(tfGroup.getText().trim());
                sftp.chgrp(absPath, gid);
            }
        } catch (NumberFormatException ignored) {}
    }
}
