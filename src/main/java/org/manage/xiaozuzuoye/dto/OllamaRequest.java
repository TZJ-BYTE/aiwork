package org.manage.xiaozuzuoye.dto;

import java.util.List;

public class OllamaRequest {
    private String prompt;
    private String model;
    private boolean stream;
    private List<ChatMessage> messages;
    private String context;
    
    // Default constructor
    public OllamaRequest() {}
    
    // Getters and setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
} 