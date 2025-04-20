package org.manage.xiaozuzuoye.controller;

import org.manage.xiaozuzuoye.dto.OllamaRequest;
import org.manage.xiaozuzuoye.dto.OllamaResponse;
import org.manage.xiaozuzuoye.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    private final AIService aiService;
    
    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }
    
    @PostMapping("/chat")
    public OllamaResponse chat(@RequestBody OllamaRequest request) {
        return aiService.getAIResponse(request);
    }
} 