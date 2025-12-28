package com.mygit.command;

import java.util.Arrays;
import java.util.List;

import com.mygit.RefsUtil;
import com.mygit.storage.ObjectStore;
import com.mygit.storage.ObjectStore.ParseObject;

public class LogCommand {
    private final ObjectStore store;

    public LogCommand() {
        this.store = ObjectStore.openDefault();
    }

    public void run() {
        String headsha = RefsUtil.readHEAD();
        if (headsha == null) {
            System.out.println("No commits (HEAD missing)");
            return;
        }

        String cur = headsha;
        while (cur != null && !cur.isBlank()) {
            byte[] storeBytes = store.readObject(cur);
            ParseObject po = store.parseObject(storeBytes);
            if (!"commit".equals(po.type)) {
                System.out.println("Object " + cur + " is not a commit (type=" + po.type + ")");
                break;
            }

            String body = new String(po.data);

            String[] parts = body.split("\n\n", 2);
            String header = parts.length > 0 ? parts[0] : "";
            String message = parts.length > 0 ? parts[1] : "";
            String tree = null;
            String parent = null;
            String author = null;
            List<String> lines = Arrays.asList(header.split("\n"));
            for (String line : lines) {
                if (line.startsWith("tree "))
                    tree = line.substring(5).trim();
                else if (line.startsWith("parent "))
                    parent = line.substring(7).trim();
                else if (line.startsWith("author "))
                    author = line.substring(7).trim();
            }

            System.out.println("commit " + cur);
            if (author != null)
                System.out.println("Author: " + author);
            System.out.println();
            System.out.println("    " + message.replace("\n", "\n    "));
            System.out.println();

            cur = parent;
        }
    }
}
