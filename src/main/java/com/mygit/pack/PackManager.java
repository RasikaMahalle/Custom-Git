package com.mygit.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PackManager {
    public static class PackBundle {
        public final PackIndex index;
        public final PackFile pack;

        PackBundle(PackIndex index, PackFile pack) {
            this.index = index;
            this.pack = pack;
        }
    }

    private final List<PackBundle> bundles = new ArrayList<>();

    public PackManager(Path packDir) {
        load(packDir);
    }

    private void load(Path packDir) {
        try {
            if (!Files.exists(packDir))
                return;

            Files.list(packDir).filter(p -> p.toString().endsWith(".idx")).forEach(idx -> {
                Path pack = idx.resolveSibling(
                        idx.getFileName().toString().replace(".idx", ".pack"));
                if (Files.exists(pack)) {
                    bundles.add(new PackBundle(new PackIndex(idx), new PackFile(pack)));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load packs", e);
        }
    }

    public byte[] readFromPacks(String sha) {
        for (PackBundle b : bundles) {
            if (b.index.contains(sha)) {
                long offset = b.index.offsetOf(sha);
                return b.pack.readObjectAt(offset);
            }
        }
        return null;
    }
}
