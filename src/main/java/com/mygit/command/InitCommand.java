package com.mygit.command;

import java.nio.file.*;

public class InitCommand {

    public void run() {
        try {
            Path root = Paths.get(".mygit");
            if (Files.exists(root)) {
                System.out.println("Repository already exists");
                return;
            }

            Files.createDirectories(root.resolve("objects"));
            Files.createDirectories(root.resolve("refs").resolve("heads"));

            Files.writeString(root.resolve("HEAD"), "ref: refs/heads/main\n");

            System.out.println("Initialized empty MyGit repository.");
        } catch (Exception e) {
            throw new RuntimeException("Init failed", e);
        }
    }

}
