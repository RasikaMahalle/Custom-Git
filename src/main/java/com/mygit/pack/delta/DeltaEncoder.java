package com.mygit.pack.delta;

import java.util.ArrayList;
import java.util.List;

public class DeltaEncoder {
    private static final int MIN_MATCH = 8;

    public static List<DeltaInstruction> encode(byte[] base, byte[] target) {
        List<DeltaInstruction> ops = new ArrayList<>();
        int i = 0;

        while (i < target.length) {
            int bestLen = 0;
            int bestOff = -1;

            for (int j=0; j < base.length - MIN_MATCH; j++){
            int len = 0;
            while (i + len < target.length && j + len < base.length && target[i + len] == base[j + len]) {
                len++;
                }
                if (len > bestLen) {
                    bestLen = len;
                    bestOff = j;
                }
            }
            if (bestLen >= MIN_MATCH) {
                ops.add(new CopyInstruction(bestOff, bestLen));
                i += bestLen;
            } else {
                ops.add(new InsertInstruction(new byte[]{target[i]}));
                i++;
            }
        }
        return ops;
    }
}
