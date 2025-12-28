package com.mygit.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigCommand {

    public void run(String[] args) {
        if (args.length != 2) {
            throw new RuntimeException(
                    "Usage: config user.name|user.email <value>");
        }

        Path config = Paths.get(".mygit").resolve("config");
        try {
            Files.writeString(config,
                    "[user]\n" +
                            args[0].substring(5) + " = " + args[1] + "\n",
                    Files.exists(config)
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write config", e);
        }
    }
}
