package com.mygit.pack.delta;

public class InsertInstruction extends DeltaInstruction {
    public final byte[] data;

    public InsertInstruction(byte[] data) {
        this.data = data;
    }

    @Override
    public byte type() {
        return INSERT;
    }
}
