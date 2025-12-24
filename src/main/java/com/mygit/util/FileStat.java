package com.mygit.util;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class FileStat {
    public long size;
    public long mtime;
    public long ctime;
    public boolean executable;

    public static FileStat fromPath(Path p) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            FileStat s = new FileStat();
            s.size = attrs.size();
            s.mtime = attrs.lastModifiedTime().toMillis();

            try {
                FileTime ft = (FileTime) Files.getAttribute(p, "creationTime", LinkOption.NOFOLLOW_LINKS);
                s.ctime = ft.toMillis();
            } catch (Exception ex) {
                s.ctime = attrs.creationTime().toMillis();
            }

            s.executable = Files.isExecutable(p);
            return s;

        } catch (Exception e) {
            throw new RuntimeException("Failed to stat file: " + p, e);
        }
    }

}
