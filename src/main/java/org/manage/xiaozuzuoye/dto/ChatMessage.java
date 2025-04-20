package org.manage.xiaozuzuoye.dto;

public class ChatMessage {
    private String role;  // "user" 或 "assistant"
    private String content;
    
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
} 