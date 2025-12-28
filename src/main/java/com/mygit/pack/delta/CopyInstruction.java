package com.mygit.pack.delta;

public class CopyInstruction extends DeltaInstruction {
    public final int offset;
    public final int length;

    public CopyInstruction(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte type() {
        return COPY;
    }
}
