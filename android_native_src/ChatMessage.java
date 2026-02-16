package com.example.jamiya;

public class ChatMessage {
    private String text;
    private boolean isUser; // true = User, false = AI

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}