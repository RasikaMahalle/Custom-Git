package com.mygit.ignore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IgnoreMatcher {
    private final List<Pattern> patterns = new ArrayList<>();

    public IgnoreMatcher(Path repoRoot) {
        loadIgnoreFile(repoRoot.resolve(".mygitignore"));
    }

    private void loadIgnoreFile(Path ignoreFile) {
        if (!Files.exists(ignoreFile))
            return;

        try {
            List<String> lines = Files.readAllLines(ignoreFile);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                patterns.add(convertToRegex(line));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ignore file", e);
        }
    }

    private Pattern convertToRegex(String rule) {
        // Directory ignore
        if (rule.endsWith("/")) {
            rule = rule + "**";
        }

        // Escape regex chars except *
        rule = rule.replace(".", "\\.")
                .replace("?", "\\?")
                .replace("+", "\\+")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("|", "\\|")
                .replace("(", "\\(")
                .replace(")", "\\)");

        // Wildcard support
        rule = rule.replace("**", ".*");
        rule = rule.replace("*", "[^/]*");

        return Pattern.compile("^" + rule + "$");
    }

    public boolean isIgnored(String relativePath) {
        relativePath = relativePath.replace("\\", "/");
        for (Pattern p : patterns) {
            if (p.matcher(relativePath).matches()) {
                return true;
            }
        }
        return false;
    }
}
