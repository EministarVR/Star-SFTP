package dev.eministar.starsftp.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eministar.starsftp.model.Profile;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class ProfileStore {
    private final File dir = new File(System.getProperty("user.home"), ".star-sftp");
    private final File json = new File(dir, "profiles.json");
    private final File meta = new File(dir, "meta.properties");
    private final ObjectMapper om = new ObjectMapper();

    private Map<String, Profile> cache = new LinkedHashMap<>();

    public ProfileStore() { load(); }

    public Map<String, Profile> profiles() { return Collections.unmodifiableMap(cache); }

    public void save(Profile p) {
        ensureDir();
        cache.put(p.name(), p);
        persist();
    }

    public void delete(String name) {
        ensureDir();
        cache.remove(name);
        persist();
    }

    public Optional<String> lastProfile() {
        try {
            if (!meta.isFile()) return Optional.empty();
            Properties props = new Properties();
            try (var in = Files.newInputStream(meta.toPath())) { props.load(in); }
            String v = props.getProperty("lastProfile");
            return v == null ? Optional.empty() : Optional.of(v);
        } catch (Exception ignore) { return Optional.empty(); }
    }

    public void setLastProfile(String name) {
        ensureDir();
        Properties props = new Properties();
        props.setProperty("lastProfile", name);
        try (var out = Files.newOutputStream(meta.toPath())) { props.store(out, "star-sftp"); }
        catch (Exception ignored) {}
    }

    private void ensureDir() { if (!dir.isDirectory()) dir.mkdirs(); }

    private void load() {
        try {
            if (!json.isFile()) { cache = new LinkedHashMap<>(); return; }
            cache = om.readValue(json, new TypeReference<LinkedHashMap<String, Profile>>() {});
        } catch (Exception e) {
            cache = new LinkedHashMap<>();
        }
    }

    private void persist() {
        try {
            om.writerWithDefaultPrettyPrinter().writeValue(json, cache);
        } catch (Exception ignored) {}
    }
}
