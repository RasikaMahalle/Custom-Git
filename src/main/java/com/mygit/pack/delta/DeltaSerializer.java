package com.mygit.pack.delta;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DeltaSerializer {
    public static byte[] serialize(String baseSha, List<DeltaInstruction> ops) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("BASE " + baseSha + "\n").getBytes());

            for (DeltaInstruction op : ops) {
                if (op.type() == DeltaInstruction.COPY) {
                    CopyInstruction c = (CopyInstruction) op;
                    out.write(("C " + c.offset + " " + c.length + "\n").getBytes());
                }
                else {
                    InsertInstruction i = (InsertInstruction) op;
                    out.write(("I " + i.data.length + "\n").getBytes());
                    out.write(i.data);
                    out.write('\n');
                }
            }
            return out.toByteArray();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to serialize delta", e);
        }
    }
}
