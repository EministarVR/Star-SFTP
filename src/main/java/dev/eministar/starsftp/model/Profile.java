package dev.eministar.starsftp.model;

public record Profile(
        String name,
        String host,
        int port,
        String username,
        String password,         // nullable
        String privateKeyPath,   // nullable
        boolean remember
) { }
