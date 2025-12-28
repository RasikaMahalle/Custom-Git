package com.mygit.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Index {
    private final Path indexPath;
    private final List<IndexEntry> entries = new ArrayList<>();

    public Index(Path gitDir) {
        this.indexPath = gitDir.resolve("index");
    }

    public List<IndexEntry> getEntries() {
        return entries;
    }

    public void load() throws IOException {
        entries.clear();
        if (!Files.exists(indexPath))
            return;

        byte[] all = Files.readAllBytes(indexPath);
        ByteArrayInputStream bais = new ByteArrayInputStream(all);
        DataInputStream d = new DataInputStream(bais);

        byte[] magic = new byte[4];
        d.readFully(magic);

        String m = new String(magic, "UTF-8");
        if (!"DIRC".equals(m))
            throw new IOException("Not an index file");
        int version = d.readInt();
        if (version != 2)
            throw new IOException("Unsupported index version: " + version);

        int entryCount = d.readInt();

        int offset = 12;
        for (int i = 0; i < entryCount; i++) {
            int remaining = all.length - offset;
            ByteArrayInputStream entryStream = new ByteArrayInputStream(all, offset, remaining);
            DataInputStream ed = new DataInputStream(entryStream);

            IndexEntry e = new IndexEntry();
            e.ctimeSecs = ed.readInt();
            e.ctimeNsecs = ed.readInt();
            e.mtimeSecs = ed.readInt();
            e.mtimeNsecs = ed.readInt();
            e.dev = ed.readInt();
            e.ino = ed.readInt();
            e.mode = ed.readInt();
            e.uid = ed.readInt();
            e.gid = ed.readInt();
            e.fileSize = ed.readInt();
            e.sha1 = new byte[20];
            ed.readFully(e.sha1);
            e.flags = ed.readShort();

            ByteArrayOutputStream pathOut = new ByteArrayOutputStream();
            int consumed = 62;
            int b;

            while ((b = ed.read()) != 0) {
                if (b == -1)
                    throw new EOFException("Unexpected EOF in path");
                pathOut.write(b);
                consumed++;
            }
            consumed++;
            e.path = pathOut.toString("UTF-8");

            int entryLen = consumed;
            int pad = (8 - (entryLen % 8)) % 8;

            for (int p = 0; p < pad; p++)
                ed.readByte();
            offset += entryLen + pad;
            entries.add(e);
        }
    }

    public void write() throws IOException {
        save();
    }

    public void save() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);

        d.writeBytes("DIRC");
        d.writeInt(2);
        d.writeInt(entries.size());

        for (IndexEntry e : entries) {
            d.writeInt(e.ctimeSecs);
            d.writeInt(e.ctimeNsecs);
            d.writeInt(e.mtimeSecs);
            d.writeInt(e.mtimeNsecs);
            d.writeInt(e.dev);
            d.writeInt(e.ino);
            d.writeInt(e.mode);
            d.writeInt(e.uid);
            d.writeInt(e.gid);
            d.writeInt(e.fileSize);
            d.write(e.sha1);
            d.writeShort(e.flags & 0xFFFF);

            byte[] pathBytes = e.path.getBytes("UTF-8");
            d.write(pathBytes);
            d.writeByte(0);
            int entryLen = 62 + pathBytes.length + 1;
            int pad = (8 - (entryLen % 8)) % 8;
            for (int i = 0; i < pad; i++)
                d.writeByte(0);
        }

        byte[] body = out.toByteArray();
        byte[] checksum = com.mygit.util.HashUtils.sha1(body);
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        finalOut.write(body);
        finalOut.write(checksum);

        Path parent = indexPath.getParent();
        if (parent != null)
            Files.createDirectories(parent);
        Path tmp = indexPath.resolveSibling("index.tmp");
        Files.write(tmp, finalOut.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, indexPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, indexPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void addEntry(IndexEntry entry) {
        entries.add(entry);
    }

    public void addorReplace(IndexEntry newEntry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path.equals(newEntry.path)) {
                entries.set(i, newEntry);
                return;
            }
        }
        entries.add(newEntry);
    }

    public IndexEntry find(String path) {
        for (IndexEntry e : entries)
            if (e.path.equals(path))
                return e;
        return null;
    }

    public void remove(String path) {
        entries.removeIf(e -> e.path.equals(path));
    }

    public void clear() {
        entries.clear();
    }
}
