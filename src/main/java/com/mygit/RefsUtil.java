package com.mygit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class RefsUtil {
    private static final Path GIT_DIR = Paths.get(".mygit");

    public static String readHEAD() {
        try {
            Path head = GIT_DIR.resolve("HEAD");
            if (!Files.exists(head))
                return null;
            String raw = Files.readString(head).trim();
            if (raw.startsWith("ref: ")) {
                String refPath = raw.substring(5).trim();
                Path p = GIT_DIR.resolve(refPath);
                if (!Files.exists(p))
                    return null;
                return Files.readString(p).trim();
            } else {
                return raw;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read HEAD", e);
        }
    }

    public static String readHEADRaw() {
        try {
            Path head = GIT_DIR.resolve("HEAD");
            if (!Files.exists(head))
                return null;
            return Files.readString(head).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read HEAD", e);
        }
    }

    public static void updateRef(String ref, String sha1) {
        try {
            Path p = GIT_DIR.resolve(ref);
            Files.createDirectories(p.getParent());
            Files.writeString(p, sha1 + "\n");
        } catch (Exception e) {
            throw new RuntimeException("Failed to update ref", e);
        }
    }

    public static void updateHEADWithSha(String sha1) {
        try {
            Path head = GIT_DIR.resolve("HEAD");
            if (!Files.exists(head)) {
                Path ref = GIT_DIR.resolve("refs/heads/main");
                Files.createDirectories(ref.getParent());
                Files.writeString(ref, sha1 + "\n");
                Files.writeString(head, "ref: refs/heads/main\n");
                return;
            }
            String raw = Files.readString(head).trim();
            if (raw.startsWith("ref: ")) {
                String refPath = raw.substring(5).trim();
                updateRef(refPath, sha1);
            } else {
                Files.writeString(head, sha1 + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update HEAD", e);
        }
    }

    public static void createBranch(String name) {
        try {
            String currentSha = readHEAD();
            if (currentSha == null) {
                throw new RuntimeException("No commits yet — cannot create branch.");
            }
            Path p = GIT_DIR.resolve("refs/heads").resolve(name);
            if (Files.exists(p)) {
                throw new RuntimeException("Branch already exists." + name);
            }
            Files.createDirectories(p.getParent());
            Files.writeString(p, currentSha + "\n");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create branch", e);
        }
    }

    public static List<String> listBranches() {
        try {
            Path base = GIT_DIR.resolve("refs/heads");
            if (!Files.exists(base)) {
                return List.of();
            }
            return Files.walk(base, 1)
                    .filter(F -> !F.equals(base))
                    .map(F -> F.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to list branches", e);
        }
    }

    public static void switchBranch(String name) {
        Path ref = GIT_DIR.resolve("refs/heads").resolve(name);
        if (!Files.exists(ref)) {
            throw new RuntimeException("Branch does not exist." + name);
        }
        try {
            Files.writeString(GIT_DIR.resolve("HEAD"), "ref: refs/heads/" + name + "\n");
        } catch (Exception e) {
            throw new RuntimeException("Failed to switch branch", e);
        }
    }

    public static boolean isDetached() {
        String raw = readHEADRaw();
        return raw != null && !raw.startsWith("ref: ");
    }

    public static String resolveBranch(String branch) {
        Path ref = GIT_DIR.resolve("refs/heads").resolve(branch);
        if (Files.exists(ref)) {
            try {
                return Files.readString(ref).trim();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read branch ref", e);
            }
        }
        return null;
    }

    public static String currentBranch() {
        String raw = readHEADRaw();
        if (raw != null && raw.startsWith("ref: ")) {
            return raw.substring(raw.lastIndexOf("/") + 1);
        }
        return null;
    }
}
