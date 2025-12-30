package com.mygit.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import com.mygit.RefsUtil;
import com.mygit.pack.PackWriter;
import com.mygit.pack.ReachabilityWalker;
import com.mygit.pack.PackWriter.PackResult;

public class GCCommand {
    public void run() {
        ReachabilityWalker walker = new ReachabilityWalker();
        Set<String> reachable = walker.reachableFromRefs();
        System.out.println("Reachable Objects: " + reachable.size());

        String headRaw = RefsUtil.readHEADRaw();
        if (headRaw != null && !headRaw.startsWith("ref: ") && !headRaw.isBlank()) {
            reachable.add(headRaw.trim());
        }

        PackWriter writer = new PackWriter();
        PackResult res = writer.packObjects(reachable);
        System.out.println("Pack created: " + res.pack);
        System.out.println("Index created: " + res.idx);

        movePackedToBackup(reachable);
    }

    private void movePackedToBackup(Set<String> shas) {
        Path objectsDir = Paths.get(".mygit").resolve("objects");
        Path backupDir = objectsDir.resolve("backup");

        try {
            Files.createDirectories(backupDir);
            for (String sha : shas) {
                Path objPath = objectsDir.resolve(sha.substring(0, 2)).resolve(sha.substring(2));

                if (Files.exists(objPath)) {
                    Path target = backupDir.resolve(sha);
                    if (!Files.exists(target)) {
                        Files.move(objPath, target, StandardCopyOption.ATOMIC_MOVE);
                    }
                }

                Path dir = objectsDir.resolve(sha.substring(0, 2));
                try {
                    if (Files.isDirectory(dir) && Files.list(dir).findAny().isEmpty()) {
                        Files.deleteIfExists(dir);
                    }
                } catch (Exception ignored) {
                }
            }
            System.out.println("Moved packed loose objects to: " + backupDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed moving packed objects to backup", e);
        }
    }
}
