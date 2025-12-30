package com.mygit.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final Path CONFIG = Paths.get(".mygit").resolve("config");

    public static Map<String, String> readUser() {

        Map<String, String> out = new HashMap<>();

        if (!Files.exists(CONFIG)) {
            return out;
        }

        try {
            String section = null;
            for (String line : Files.readAllLines(CONFIG)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1);
                    continue;
                }

                if (section != null && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    out.put(section + "." + parts[0].trim(),
                            parts[1].trim());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config", e);
        }

        return out;
    }

    public static String userName() {
        return readUser().getOrDefault("user.name", "User");
    }

    public static String userEmail() {
        return readUser().getOrDefault("user.email", "user@mygit");
    }
}
