package com.mygit.pack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mygit.util.IOUtils;

public class PackFile {
    private final Path packPath;

    public PackFile(Path packPath) {
        this.packPath = packPath;
    }

    public byte[] readObjectAt(long offset) {
        try (InputStream in = Files.newInputStream(packPath)) {
            in.skip(offset);

            byte[] sha = in.readNBytes(20);
            int len = readInt(in);
            byte[] compressed = in.readNBytes(len);

            return IOUtils.zlibDecompress(compressed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read object", e);
        }
    }

    private int readInt(InputStream in) throws Exception {
        return (in.read() << 24)
                | (in.read() << 16)
                | (in.read() << 8)
                | in.read();
    }
}
