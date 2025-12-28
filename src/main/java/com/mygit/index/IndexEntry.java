package com.mygit.index;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.mygit.util.FileStat;
import com.mygit.util.HashUtils;

public class IndexEntry {
    public int ctimeSecs;
    public int ctimeNsecs;
    public int mtimeSecs;
    public int mtimeNsecs;

    public int dev;
    public int ino;
    public int mode;
    public int uid;
    public int gid;
    public int fileSize;

    public byte[] sha1;
    public short flags;

    public String path;

    public IndexEntry() {

    }

    public static IndexEntry fromFileSat(String path, FileStat stat, byte[] sha1) {
        IndexEntry e = new IndexEntry();
        e.ctimeSecs = (int) (stat.ctime / 1000);
        e.ctimeNsecs = (int) (stat.ctime % 1000) * 1000000;
        e.mtimeSecs = (int) (stat.mtime / 1000);
        e.mtimeNsecs = (int) (stat.mtime % 1000) * 1000000;
        e.dev = 0;
        e.ino = 0;
        e.mode = stat.executable ? 0100755 : 0100644;
        e.uid = 0;
        e.gid = 0;
        e.fileSize = (int) stat.size;
        e.sha1 = sha1;
        e.flags = (short) (path.length() & 0x00FFF);
        e.path = path;
        return e;
    }

    public void writeTo(OutputStream out) throws IOException {
        DataOutputStream d = new DataOutputStream(out);

        d.writeInt(ctimeSecs);
        d.writeInt(ctimeNsecs);
        d.writeInt(mtimeSecs);
        d.writeInt(mtimeNsecs);
        d.writeInt(dev);
        d.writeInt(ino);
        d.writeInt(mode);
        d.writeInt(uid);
        d.writeInt(gid);
        d.writeInt(fileSize);
        d.write(sha1);
        d.writeShort(flags & 0x00FFF);

        byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
        d.write(pathBytes);
        d.write(0);

        int entryLen = 62 + pathBytes.length + 1;
        int pad = (8 - (entryLen % 8)) % 8;
        for (int i = 0; i < pad; i++) {
            d.writeByte(0);
        }
    }

    public static IndexEntry readFrom(DataInputStream d) throws IOException {
        IndexEntry e = new IndexEntry();

        e.ctimeSecs = d.readInt();
        e.ctimeNsecs = d.readInt();
        e.mtimeSecs = d.readInt();
        e.mtimeNsecs = d.readInt();
        e.dev = d.readInt();
        e.ino = d.readInt();
        e.mode = d.readInt();
        e.uid = d.readInt();
        e.gid = d.readInt();
        e.fileSize = d.readInt();
        e.sha1 = new byte[20];
        d.readFully(e.sha1);
        e.flags = d.readShort();

        ByteArrayOutputStream pathOut = new ByteArrayOutputStream();
        byte b;
        while ((b = d.readByte()) != 0) {
            pathOut.write(b);
        }
        e.path = pathOut.toString(StandardCharsets.UTF_8.name());

        return e;
    }

    @Override
    public String toString() {
        return "IndexEntry{" +
                "path='" + path + '\'' +
                ", size=" + fileSize +
                ", mode=" + Integer.toOctalString(mode) +
                ", sha1=" + HashUtils.toHex(sha1) +
                '}';
    }

    public boolean matchesStat(FileStat stat) {
        if (this.fileSize != (int) stat.size)
            return false;
        int msecs = (int) (stat.mtime / 1000);
        return this.mtimeSecs == msecs;
    }

}
