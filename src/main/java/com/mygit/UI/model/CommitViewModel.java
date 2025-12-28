package com.mygit.UI.model;

public class CommitViewModel {

    private final String sha;
    private final String message;
    private final String author;
    private final String date;

    public CommitViewModel(String sha, String message, String author, String date) {
        this.sha = sha;
        this.message = message;
        this.author = author;
        this.date = date;
    }

    public String getSha() {
        return sha;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return sha + "  " + message + "\n" +
                "    " + author + "\n" +
                "    " + date;
    }
}
