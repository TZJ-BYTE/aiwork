package org.manage.xiaozuzuoye.service;

import org.manage.xiaozuzuoye.config.AIConfig;
import org.manage.xiaozuzuoye.dto.OllamaRequest;
import org.manage.xiaozuzuoye.dto.OllamaResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import java.time.Duration;
import reactor.util.retry.Retry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import org.manage.xiaozuzuoye.dto.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;
import java.net.ConnectException;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final String baseUrl;
    private final String aiModel;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebClient> sessionClients = new ConcurrentHashMap<>();
    
    // 用于存储每个会话的对话历史和模型实例
    private final Map<String, List<ChatMessage>> sessionHistories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int INITIAL_HISTORY_CAPACITY = 20;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final Pattern CODE_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    // 添加简单的响应缓存
    // 每个会话独立的响应缓存
    private final Map<String, Map<String, String>> sessionResponseCache = new ConcurrentHashMap<>();

    public AIService(@Value("${ai.base-url}") String baseUrl, 
                    @Value("${ai.model}") String aiModel,
                    ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.aiModel = aiModel;
        this.objectMapper = objectMapper;
        logger.info("AIService initialized with baseUrl: {}", baseUrl);
    }

    private WebClient createWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 增加连接超时到10秒
            .responseTimeout(Duration.ofSeconds(60))
            .doOnConnected(conn -> {
                logger.debug("正在连接 Ollama 服务: {}", baseUrl);
                conn.addHandlerLast(new ReadTimeoutHandler(60));
                conn.addHandlerLast(new WriteTimeoutHandler(60));
            });

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                if (clientResponse.statusCode().is5xxServerError()) {
                    return clientResponse.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException(
                            "Ollama 服务错误: " + clientResponse.statusCode() + " - " + body)));
                }
                return Mono.just(clientResponse);
            }))
            .filter(retryFilter())
            .build();
    }

    // 添加重试过滤器
    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
            .doOnError(e -> logger.error("连接异常: {} => {}", baseUrl, e.getMessage()))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof ConnectException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.error("连接重试失败: {}", baseUrl);
                    return new RuntimeException("AI服务不可用");
                }));
    }

    // 提取关键信息的方法
    private String extractKeyInfo(String content) {
        if (content == null || content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }

        // 提取所有代码块
        Matcher codeMatcher = CODE_PATTERN.matcher(content);
        StringBuilder codeBlocks = new StringBuilder();
        while (codeMatcher.find()) {
            codeBlocks.append(codeMatcher.group()).append("\n");
        }

        // 提取非代码内容的关键信息
        String nonCodeContent = content.replaceAll("```[\\s\\S]*?```", "")
                                     .replaceAll("\\s+", " ")
                                     .trim();
        
        // 智能分段
        String[] paragraphs = nonCodeContent.split("(?<=\\.)\\s+|(?<=。)\\s*|(?<=\\n)\\s*");
        StringBuilder summary = new StringBuilder();
        
        // 保留重要段落
        for (String para : paragraphs) {
            if (isImportantParagraph(para)) {
                summary.append(para.trim()).append(" ");
            }
        }

        // 组合代码块和摘要
        StringBuilder result = new StringBuilder();
        if (codeBlocks.length() > 0) {
            result.append(codeBlocks);
        }
        if (summary.length() > 0) {
            result.append(summary);
        }

        return result.length() > MAX_CONTENT_LENGTH ? 
               result.substring(0, MAX_CONTENT_LENGTH) + "..." : 
               result.toString();
    }

    // 判断段落重要性
    private boolean isImportantParagraph(String paragraph) {
        // 包含关键词的段落
        String[] keywords = {"important", "key", "note", "warning", "error", "solution", 
                           "重要", "关键", "注意", "警告", "错误", "解决方案"};
        
        // 包含代码相关的段落
        String[] codeKeywords = {"class", "function", "method", "import", "public", 
                               "private", "return", "void"};
                               
        paragraph = paragraph.toLowerCase();
        
        // 检查关键词
        for (String keyword : keywords) {
            if (paragraph.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 检查代码相关词
        for (String keyword : codeKeywords) {
            if (paragraph.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否是问题或答案的开头
        return paragraph.startsWith("q:") || paragraph.startsWith("a:") ||
               paragraph.startsWith("问:") || paragraph.startsWith("答:");
    }

    public void streamResponse(String sessionId, OllamaRequest request, Consumer<OllamaResponse> onResponse) {
        // 获取或创建会话锁
        Object sessionLock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        
        // 使用会话锁确保每个会话的请求按顺序处理
        synchronized(sessionLock) {
            request.setModel(aiModel);
            // 检查会话缓存
            String cachedResponse = getCachedResponse(sessionId, request.getPrompt());
        if (cachedResponse != null) {
                OllamaResponse response = new OllamaResponse();
                response.setResponse(cachedResponse);
                response.setDone(true);
                onResponse.accept(response);
                return;
            }

            WebClient client = sessionClients.computeIfAbsent(sessionId, k -> createWebClient());
            List<ChatMessage> history = sessionHistories.computeIfAbsent(sessionId, 
                k -> new ArrayList<>(INITIAL_HISTORY_CAPACITY));
        
            StringBuilder fullPrompt = new StringBuilder(4096);  // 增加初始容量
            if (!history.isEmpty()) {
                fullPrompt.append("Previous conversation:\n\n");
            // 保持完整的对话历史
            for (int i = 0; i < history.size(); i += 2) {
                ChatMessage question = history.get(i);
                ChatMessage answer = history.get(i + 1);
                // 不再提取关键信息，保持完整内容
                fullPrompt.append("User: ").append(question.getContent()).append("\n\n");
                fullPrompt.append("Assistant: ").append(answer.getContent()).append("\n\n");
            }
            fullPrompt.append("Current user question:\n");
        }
        fullPrompt.append(request.getPrompt());
        
        request.setPrompt(fullPrompt.toString());
        request.setStream(true);
        
            client.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .map(chunk -> {
                    try {
                        OllamaResponse response = objectMapper.readValue(chunk, OllamaResponse.class);
                        if (response.isDone()) {
                            if (history.size() >= MAX_HISTORY_SIZE * 2) {
                                history.subList(0, 2).clear();
                            }
                            history.add(new ChatMessage("user", request.getPrompt()));
                            history.add(new ChatMessage("assistant", response.getResponse()));
                            // 缓存响应
                            cacheResponse(sessionId, request.getPrompt(), response.getResponse());
                        }
                        return response;
                    } catch (Exception e) {
                        logger.error("解析响应出错", e);
                        return new OllamaResponse() {{
                            setResponse("解析错误: " + e.getMessage());
                            setDone(true);
                        }};
                    }
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .subscribe(onResponse);
         }
    }

    public void closeSession(String sessionId) {
        sessionClients.remove(sessionId);
        sessionHistories.remove(sessionId);
        logger.info("已关闭会话: {}", sessionId);
    }

    public OllamaResponse getAIResponse(OllamaRequest request) {
        request.setModel(aiModel);
        request.setStream(false);
        try {
            return createWebClient()
                    .post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block(Duration.ofSeconds(30));
        } catch (Exception e) {
            logger.error("AI service error: ", e);
            OllamaResponse errorResponse = new OllamaResponse();
            errorResponse.setResponse("抱歉，AI服务暂时无法响应: " + e.getMessage());
            errorResponse.setDone(true);
            return errorResponse;
        }
    }

    @PostConstruct
    public void init() {
        logger.info("正在预热AI模型...");
        
        try {
            OllamaRequest warmupRequest = new OllamaRequest();
            warmupRequest.setModel(aiModel);
            warmupRequest.setStream(false);
            
            Mono.defer(() -> createWebClient()
                .post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(warmupRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    response.bodyToMono(String.class).map(body -> 
                        new RuntimeException("Ollama 服务内部错误: " + body)))
                .bodyToMono(OllamaResponse.class))
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof ConnectException)
                    .doBeforeRetry(signal -> 
                        logger.warn("正在重试连接 Ollama 服务... 第{}次", signal.totalRetries() + 1)))
                .subscribe(
                    response -> logger.info("AI模型预热完成"),
                    error -> logger.warn("AI模型预热失败，系统将在没有AI功能的情况下运行: {}", error.getMessage())
                );
        } catch (Exception e) {
            logger.warn("AI服务初始化异常，系统将在没有AI功能的情况下运行: {}", e.getMessage());
        }
    }

    private String getCachedResponse(String sessionId, String prompt) {
        Map<String, String> sessionCache = sessionResponseCache.get(sessionId);
        return sessionCache != null ? sessionCache.get(prompt) : null;
    }

    private void cacheResponse(String sessionId, String prompt, String response) {
        sessionResponseCache.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                           .put(prompt, response);
    }
}