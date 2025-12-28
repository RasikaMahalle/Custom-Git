package com.mygit.pack.delta;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DeltaDecoder {
    public static byte[] apply(byte[] base, List<DeltaInstruction> ops) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (DeltaInstruction op : ops) {
                if (op.type() == DeltaInstruction.COPY) {
                    CopyInstruction c = (CopyInstruction) op;
                    out.write(base, c.offset, c.length);
                }
                else {
                    InsertInstruction i = (InsertInstruction) op;
                    out.write(i.data);
                }
            }
            return out.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Delta apply failed", e);
        }
    }
}
