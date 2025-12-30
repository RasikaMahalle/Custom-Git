package com.mygit.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mygit.command.ResetCommand;
import com.mygit.command.StatusCommand;

public class StashCommand {

    public void run(String[] args) {
        if (args.length == 1) {
            String msg = push();
            System.out.println(msg);
            return;
        }

        if (args.length == 2) {
            if ("apply".equals(args[1])) {
                String msg = apply(false);
                System.out.println(msg);
                return;
            }
            if ("pop".equals(args[1])) {
                String msg = apply(true);
                System.out.println(msg);
                return;
            }
        }

        System.out.println("Usage: mygit stash [apply|pop]");
    }

    public String push() {
        StatusCommand status = new StatusCommand();
        if (!status.isDirty()) {
            return "No local changes to stash.";
        }

        Set<String> dirtyFiles = status.getModifiedTrackedFiles();

        if (dirtyFiles.isEmpty()) {
            return "No local changes to stash.";
        }

        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        Path gitDir = repoRoot.resolve(".mygit");
        Path stashDir = gitDir.resolve("stash");

        deleteDirectory(stashDir);

        try {
            for (String path : dirtyFiles) {
                Path src = repoRoot.resolve(path).normalize();
                if (Files.exists(src) && Files.isRegularFile(src)) {
                    Path dest = stashDir.resolve(path).normalize();
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            new ResetCommand().run(new String[] { "--hard" });
        } catch (Exception e) {
            deleteDirectory(stashDir);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to save stash", e);
        }

        return "Saved working tree and index state to stash.";
    }

    public String apply(boolean drop) {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        Path gitDir = repoRoot.resolve(".mygit");
        Path stashDir = gitDir.resolve("stash");

        if (!Files.exists(stashDir) || !Files.isDirectory(stashDir)) {
            throw new RuntimeException("No stash found.");
        }

        try (Stream<Path> stream = Files.walk(stashDir)) {
            Set<Path> files = stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toSet());

            if (files.isEmpty()) {
                throw new RuntimeException("No stash found.");
            }

            for (Path file : files) {
                Path rel = stashDir.relativize(file);
                Path dest = repoRoot.resolve(rel);
                Files.createDirectories(dest.getParent());
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply stash", e);
        }

        if (drop) {
            deleteDirectory(stashDir);
            return "Applied and dropped latest stash.";
        }

        return "Applied latest stash (stash kept).";
    }

    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear stash directory", e);
        }
    }
}
