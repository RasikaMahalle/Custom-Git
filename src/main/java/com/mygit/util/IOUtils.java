package com.mygit.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class IOUtils {
    public static byte[] zlibCompress(byte[] input) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(input);
            deflater.finish();

            byte[] buffer = new byte[8192];
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    public static byte[] zlibDecompress(byte[] input) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(input);

            byte[] buffer = new byte[8192];
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }

    public static void writeFile(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (Exception e) {
            throw new RuntimeException("File write failed", e);
        }
    }

    public static byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("File read failed", e);
        }
    }
}
