package com.mygit.storage;

public class CommitObj {
    public final String tree;
    public final String parent;
    public final String author;
    public final String message;

    public CommitObj(String tree, String parent, String author, String message) {
        this.tree = tree;
        this.parent = parent;
        this.author = author;
        this.message = message;
    }

    @Override
    public String toString() {
        return "CommitObj{" +
                "tree='" + tree + '\'' +
                ", parent='" + parent + '\'' +
                ", author='" + author + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
