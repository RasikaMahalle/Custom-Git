package com.mygit.pack.delta;

public abstract class DeltaInstruction {
    public static final byte COPY = 'C';
    public static final byte INSERT = 'I';

    public abstract byte type();
}
