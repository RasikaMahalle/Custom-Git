package com.mygit.model;

import java.nio.file.Path;

public class FileMeta {
    public Path path;
    public long size;
    public long mtime;
    public String sha1;

    public FileMeta(Path path, long size, long mtime, String sha1) {
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.sha1 = sha1;
    }
}
