package org.manage.xiaozuzuoye.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {
    private String response;
    private String model;
    private boolean done;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("done_reason")
    private String doneReason;
    
    private List<Integer> context;
    
    @JsonProperty("total_duration")
    private Long totalDuration;
    
    @JsonProperty("load_duration")
    private Long loadDuration;
    
    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;
    
    @JsonProperty("prompt_eval_duration")
    private Long promptEvalDuration;
    
    @JsonProperty("eval_count")
    private Integer evalCount;
    
    @JsonProperty("eval_duration")
    private Long evalDuration;

    // Default constructor
    public OllamaResponse() {}
    
    // Getters and setters
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getDoneReason() {
        return doneReason;
    }
    
    public void setDoneReason(String doneReason) {
        this.doneReason = doneReason;
    }
    
    public List<Integer> getContext() {
        return context;
    }
    
    public void setContext(List<Integer> context) {
        this.context = context;
    }
    
    public Long getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public Long getLoadDuration() {
        return loadDuration;
    }
    
    public void setLoadDuration(Long loadDuration) {
        this.loadDuration = loadDuration;
    }
    
    public Integer getPromptEvalCount() {
        return promptEvalCount;
    }
    
    public void setPromptEvalCount(Integer promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }
    
    public Long getPromptEvalDuration() {
        return promptEvalDuration;
    }
    
    public void setPromptEvalDuration(Long promptEvalDuration) {
        this.promptEvalDuration = promptEvalDuration;
    }
    
    public Integer getEvalCount() {
        return evalCount;
    }
    
    public void setEvalCount(Integer evalCount) {
        this.evalCount = evalCount;
    }
    
    public Long getEvalDuration() {
        return evalDuration;
    }
    
    public void setEvalDuration(Long evalDuration) {
        this.evalDuration = evalDuration;
    }
} 