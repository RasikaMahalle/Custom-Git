package com.mygit.pack.delta;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DeltaDeserializer {
    public static class DeltaData {
        public final String baseSha;
        public final List<DeltaInstruction> ops;

        public DeltaData(String baseSha, List<DeltaInstruction> ops) {
            this.baseSha = baseSha;
            this.ops = ops;
        }
    }

    public static DeltaData deserialize(byte[] data) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));

            String baseLine = br.readLine();
            String baseSha = baseLine.substring(5).trim();

            List<DeltaInstruction> ops = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("C ")) {
                    String[] p = line.split(" ");
                    ops.add(
                            new CopyInstruction(Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                } else if (line.startsWith("I ")) {
                    int len = Integer.parseInt(line.substring(2));
                    char[] buf = new char[len];
                    br.read(buf, 0, len);
                    br.read();
                    ops.add(new InsertInstruction(new String(buf).getBytes()));
                }
            }
            return new DeltaData(baseSha, ops);
        } catch (Exception e) {
            throw new RuntimeException("Delta deserialize failed", e);
        }
    }
}
