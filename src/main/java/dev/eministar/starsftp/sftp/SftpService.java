package dev.eministar.starsftp.sftp;

import dev.eministar.starsftp.model.Profile;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SftpService {
    private SSHClient ssh;
    private SFTPClient sftp;
    private String currentDir = "/";

    public void connect(Profile p) throws Exception {
        close();
        ssh = new SSHClient();
        // DEV: unsicher – erlaubt alle Hostkeys. Für Prod: OpenSSHKnownHosts nutzen.
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect(p.host(), p.port());

        if (p.privateKeyPath() != null && !p.privateKeyPath().isBlank()) {
            File key = new File(p.privateKeyPath());
            if (!key.isFile()) throw new IllegalArgumentException("Private Key nicht gefunden: " + key);
            if (p.password() != null && !p.password().isBlank()) {
                ssh.authPublickey(p.username(), ssh.loadKeys(key.getAbsolutePath(), p.password().toCharArray()));
            } else {
                ssh.authPublickey(p.username(), ssh.loadKeys(key.getAbsolutePath()));
            }
        } else if (p.password() != null && !p.password().isBlank()) {
            ssh.authPassword(p.username(), p.password());
        } else {
            throw new IllegalArgumentException("Kein Auth-Mechanismus (Passwort/Key) angegeben.");
        }

        sftp = ssh.newSFTPClient();
        currentDir = sftp.canonicalize(".");
    }

    public boolean isConnected() { return sftp != null; }
    public String getCurrentDir() { return currentDir; }

    public List<RemoteItem> list(String path) throws IOException {
        ensure();
        String base = (path == null || path.isBlank()) ? currentDir : sftp.canonicalize(path);
        currentDir = base;

        List<RemoteItem> out = new ArrayList<>();
        for (RemoteResourceInfo r : sftp.ls(base)) {
            String name = r.getName();
            if (".".equals(name) || "..".equals(name)) continue;
            boolean dir = r.isDirectory();
            long size = dir ? -1L : r.getAttributes().getSize();
            long mtime = r.getAttributes().getMtime();
            String full = base.endsWith("/") ? base + name : base + "/" + name;
            out.add(new RemoteItem(full, name, dir, size, mtime > 0 ? mtime * 1000L : 0L));
        }
        return out;
    }

    public void upload(File localFile, String remoteDir) throws IOException {
        ensure();
        if (!localFile.isFile()) throw new IllegalArgumentException("Lokale Datei nicht gefunden: " + localFile);
        String target = remoteDir.endsWith("/") ? remoteDir + localFile.getName() : remoteDir + "/" + localFile.getName();
        sftp.put(localFile.getAbsolutePath(), target);
    }

    public FileAttributes stat(String absPath) throws Exception {
        ensure();
        return sftp.stat(absPath);

    }

    public void setattr(String absPath, FileAttributes attrs) throws Exception {
        ensure();
        sftp.setattr(absPath, attrs);
    }

    public void download(String remotePath, File localDir) throws IOException {
        ensure();
        if (!localDir.isDirectory()) throw new IllegalArgumentException("Lokales Ziel ist kein Ordner: " + localDir);
        String name = remotePath.substring(remotePath.lastIndexOf('/') + 1);
        File target = new File(localDir, name);
        sftp.get(remotePath, target.getAbsolutePath());
    }

    public void mkdir(String remoteDir) throws IOException { ensure(); sftp.mkdir(remoteDir); }

    public void rename(String from, String to) throws IOException { ensure(); sftp.rename(from, to); }

    public void delete(String remotePath, boolean recursive) throws IOException {
        ensure();
        try {
            for (RemoteResourceInfo r : sftp.ls(remotePath)) {
                String n = r.getName();
                if (".".equals(n) || "..".equals(n)) continue;
                String child = remotePath.endsWith("/") ? remotePath + n : remotePath + "/" + n;
                if (r.isDirectory()) {
                    if (!recursive) throw new IOException("Ist ein Verzeichnis: " + remotePath);
                    delete(child, true);
                } else sftp.rm(child);
            }
            sftp.rmdir(remotePath);
        } catch (IOException notDir) {
            sftp.rm(remotePath);
        }
    }

    public void chmod(String path, int perms) throws Exception {
        sftp.chmod(path, perms);
    }
    public void chown(String path, int uid) throws Exception {
        sftp.chown(path, uid);
    }
    public void chgrp(String path, int gid) throws Exception {
        sftp.chgrp(path, gid);
    }

    public void close() {
        try { if (sftp != null) sftp.close(); } catch (Exception ignored) {}
        try { if (ssh != null) ssh.disconnect(); } catch (Exception ignored) {}
        try { if (ssh != null) ssh.close(); } catch (Exception ignored) {}
        sftp = null; ssh = null; currentDir = "/";
    }

    private void ensure() {
        if (sftp == null) throw new IllegalStateException("Nicht verbunden");
    }

    public static record RemoteItem(String path, String name, boolean directory, long size, long modifiedEpochMs) { }
}
