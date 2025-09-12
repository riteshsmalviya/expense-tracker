package com.ritesh.expensetracker.controller.AI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritesh.expensetracker.model.Expense;
import com.ritesh.expensetracker.service.ExpenseService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);
    
    private RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.key}")
    private String deepSeekApiKey;

    @Value("${deepseek.api.url}")
    private String deepSeekApiUrl;

    @Value("${deepseek.api.timeout.connect:120}")
    private int connectTimeoutSeconds;

    @Value("${deepseek.api.timeout.read:120}")
    private int readTimeoutSeconds;

    // Simple cache for analytics
    private Map<String, Object> cachedAnalytics;
    private LocalDateTime lastCacheUpdate;
    private static final int CACHE_MINUTES = 10;

    public AiController() {
        this.objectMapper = new ObjectMapper();
    }

    @Autowired
    private ExpenseService expenseService;

    // Initialize RestTemplate with configurable timeouts after Spring injection
    @jakarta.annotation.PostConstruct
    private void initializeRestTemplate() {
        logger.info("Configuring RestTemplate with timeouts: connect={}s, read={}s", 
                   connectTimeoutSeconds, readTimeoutSeconds);
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }

    @PostMapping("/quickInsight")
    public ResponseEntity<String> getQuickInsights(@RequestBody String entity) {
        try {
            logger.info("Processing quick AI insight request: {}", entity);
            
            // Get cached analytics for faster response
            Map<String, Object> analytics = getCachedAnalytics();
            
            // Create lightweight prompt using aggregated data
            String prompt = createQuickPrompt(analytics, entity);
            logger.debug("Created quick prompt with length: {}", prompt.length());

            // Call API with smaller payload
            String aiResponse = callDeepSeekApi(prompt);
            logger.info("Successfully received quick AI response");

            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            logger.error("Error generating quick AI insights", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating quick insights: " + e.getMessage());
        }
    }

    private Map<String, Object> getCachedAnalytics() {
        // Simple in-memory cache implementation
        if (cachedAnalytics == null || shouldRefreshCache()) {
            refreshAnalyticsCache();
        }
        return cachedAnalytics != null ? cachedAnalytics : new HashMap<>();
    }

    private boolean shouldRefreshCache() {
        return lastCacheUpdate == null || 
               lastCacheUpdate.plusMinutes(CACHE_MINUTES).isBefore(LocalDateTime.now());
    }

    private void refreshAnalyticsCache() {
        try {
            List<Expense> allExpenses = expenseService.getAllExpenses();
            cachedAnalytics = generateAnalyticsSummary(allExpenses);
            lastCacheUpdate = LocalDateTime.now();
            logger.info("Analytics cache refreshed with {} expenses", allExpenses.size());
        } catch (Exception e) {
            logger.error("Failed to refresh analytics cache", e);
            cachedAnalytics = new HashMap<>();
        }
    }

    private Map<String, Object> generateAnalyticsSummary(List<Expense> expenses) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (expenses.isEmpty()) {
            return analytics;
        }

        double totalAmount = expenses.stream().mapToDouble(Expense::getAmount).sum();
        analytics.put("totalAmount", totalAmount);
        analytics.put("totalCount", expenses.size());
        analytics.put("averageAmount", totalAmount / expenses.size());

        // Category breakdown (top 10)
        Map<String, Double> categoryTotals = expenses.stream()
            .collect(Collectors.groupingBy(
                Expense::getCategory,
                Collectors.summingDouble(Expense::getAmount)
            ));
        analytics.put("categoryTotals", categoryTotals);

        // Recent month trend
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        double recentTotal = expenses.stream()
            .filter(e -> {
                try {
                    return LocalDate.parse(e.getDate()).isAfter(oneMonthAgo);
                } catch (DateTimeParseException ex) {
                    return false;
                }
            })
            .mapToDouble(Expense::getAmount)
            .sum();
        analytics.put("recentMonthTotal", recentTotal);

        return analytics;
    }

    private String createQuickPrompt(Map<String, Object> analytics, String userQuery) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on my expense summary, answer: ").append(userQuery).append("\n\n");
        
        if (analytics.isEmpty()) {
            prompt.append("No expense data available.");
            return prompt.toString();
        }

        prompt.append("EXPENSE OVERVIEW:\n");
        prompt.append(String.format("Total Spent: $%.2f\n", (Double) analytics.getOrDefault("totalAmount", 0.0)));
        prompt.append(String.format("Total Transactions: %d\n", (Integer) analytics.getOrDefault("totalCount", 0)));
        prompt.append(String.format("Average per Transaction: $%.2f\n", (Double) analytics.getOrDefault("averageAmount", 0.0)));
        prompt.append(String.format("Recent Month: $%.2f\n", (Double) analytics.getOrDefault("recentMonthTotal", 0.0)));

        @SuppressWarnings("unchecked")
        Map<String, Double> categoryTotals = (Map<String, Double>) analytics.get("categoryTotals");
        if (categoryTotals != null && !categoryTotals.isEmpty()) {
            prompt.append("\nTOP SPENDING CATEGORIES:\n");
            categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> prompt.append(String.format("- %s: $%.2f\n", entry.getKey(), entry.getValue())));
        }

        prompt.append("\nProvide concise insights and recommendations.");
        return prompt.toString();
    }

    @PostMapping("/refreshCache")
    public ResponseEntity<String> refreshCache() {
        try {
            refreshAnalyticsCache();
            return ResponseEntity.ok("Analytics cache refreshed successfully");
        } catch (Exception e) {
            logger.error("Failed to refresh cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to refresh cache: " + e.getMessage());
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        try {
            Map<String, Object> analytics = getCachedAnalytics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Failed to get analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get analytics: " + e.getMessage()));
        }
    }

    // Enum for query types
    private enum QueryType {
        SPENDING_TRENDS, CATEGORY_ANALYSIS, BUDGET_INSIGHTS, RECENT_ACTIVITY, GENERAL
    }

    @PostMapping("/aiInsight")
    public ResponseEntity<String> getAiInsights(@RequestBody String entity) {
        try {
            logger.info("Processing AI insight request with query: {}", entity);
            
            // Get all expenses
            List<Expense> allExpenses = expenseService.getAllExpenses();
            logger.info("Retrieved {} expenses from database", allExpenses.size());

            // Analyze query type and get relevant data
            QueryType queryType = analyzeQueryType(entity);
            List<Expense> relevantExpenses = getRelevantExpenses(entity, allExpenses, queryType);
            logger.info("Filtered to {} relevant expenses", relevantExpenses.size());

            // Create optimized prompt
            String prompt = createSmartPrompt(relevantExpenses, entity, queryType);
            logger.debug("Created optimized prompt with length: {}", prompt.length());

            // Call DeepSeek API
            String aiResponse = callDeepSeekApi(prompt);
            logger.info("Successfully received AI response");

            return ResponseEntity.ok(aiResponse);

        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body("API Error: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            logger.error("REST Client Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service unavailable: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error generating AI insights", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating AI insights: " + e.getMessage());
        }
    }

    private String callDeepSeekApi(String prompt) throws Exception {
        logger.info("Making optimized API call to DeepSeek: {}", deepSeekApiUrl);
        logger.debug("Prompt length: {} characters", prompt.length());
        
        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekApiKey);
        
        // Optional: OpenRouter specific headers
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "Expense Tracker AI");
        
        // Create optimized request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek/deepseek-r1-0528:free");
        requestBody.put("max_tokens", 800); // Reduced for faster response
        requestBody.put("temperature", 0.7);
        
        // Create messages array
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);
        
        logger.debug("Request body size: {} bytes", objectMapper.writeValueAsString(requestBody).length());
        
        // Create HTTP entity
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            logger.info("Starting API call with timeouts: connect={}s, read={}s", 
                       connectTimeoutSeconds, readTimeoutSeconds);
            long startTime = System.currentTimeMillis();
            
            // Make API call with extended timeout
            ResponseEntity<String> response = restTemplate.exchange(
                    deepSeekApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            
            long endTime = System.currentTimeMillis();
            logger.info("API call completed in {} ms", (endTime - startTime));
            logger.debug("API Response status: {}", response.getStatusCode());
            logger.debug("API Response received, length: {}", 
                        response.getBody() != null ? response.getBody().length() : 0);
            
            // Parse response to extract content
            return extractContentFromResponse(response.getBody());
            
        } catch (HttpClientErrorException e) {
            logger.error("API call failed with status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (RestClientException e) {
            logger.error("REST Client Error (possible timeout): {}", e.getMessage(), e);
            throw new Exception("API request failed - possibly due to timeout or connection issues: " + e.getMessage());
        }
    }

    private String extractContentFromResponse(String responseBody) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // Check if response has the expected structure
            if (!jsonNode.has("choices") || jsonNode.path("choices").isEmpty()) {
                throw new Exception("Invalid response structure: missing choices array");
            }
            
            JsonNode firstChoice = jsonNode.path("choices").get(0);
            if (!firstChoice.has("message")) {
                throw new Exception("Invalid response structure: missing message in choice");
            }
            
            JsonNode messageNode = firstChoice.path("message");
            
            // Try to get content from the "content" field first
            String content = messageNode.path("content").asText();
            
            // If content is empty, try to get it from the "reasoning" field (for deepseek-r1 models)
            if (content == null || content.trim().isEmpty()) {
                content = messageNode.path("reasoning").asText();
                logger.debug("Content was empty, using reasoning field instead");
            }
            
            // If still empty, throw an exception
            if (content == null || content.trim().isEmpty()) {
                logger.error("Both content and reasoning fields are empty in API response");
                throw new Exception("Empty content in API response");
            }
            
            logger.debug("Successfully extracted content with length: {}", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("Error parsing response: {}", responseBody, e);
            throw new Exception("Failed to parse API response: " + e.getMessage());
        }
    }

    // Analyze what type of query the user is asking
    private QueryType analyzeQueryType(String query) {
        String q = query.toLowerCase();
        if (q.contains("trend") || q.contains("pattern") || q.contains("over time")) {
            return QueryType.SPENDING_TRENDS;
        }
        if (q.contains("category") || q.contains("type") || q.contains("breakdown")) {
            return QueryType.CATEGORY_ANALYSIS;
        }
        if (q.contains("budget") || q.contains("saving") || q.contains("reduce") || q.contains("cut")) {
            return QueryType.BUDGET_INSIGHTS;
        }
        if (q.contains("recent") || q.contains("today") || q.contains("week") || q.contains("month")) {
            return QueryType.RECENT_ACTIVITY;
        }
        return QueryType.GENERAL;
    }

    // Get relevant expenses based on query type and content
    private List<Expense> getRelevantExpenses(String userQuery, List<Expense> allExpenses, QueryType queryType) {
        String query = userQuery.toLowerCase();
        
        switch (queryType) {
            case RECENT_ACTIVITY:
                return getRecentExpenses(allExpenses, query);
            case CATEGORY_ANALYSIS:
                return getCategorySpecificExpenses(allExpenses, query);
            case SPENDING_TRENDS:
                return getTrendAnalysisExpenses(allExpenses);
            case BUDGET_INSIGHTS:
                return getBudgetRelevantExpenses(allExpenses);
            default:
                // For general queries, return recent expenses limited to 50
                return allExpenses.stream()
                    .sorted((a, b) -> {
                        try {
                            return LocalDate.parse(b.getDate()).compareTo(LocalDate.parse(a.getDate()));
                        } catch (DateTimeParseException e) {
                            return b.getDate().compareTo(a.getDate());
                        }
                    })
                    .limit(50)
                    .collect(Collectors.toList());
        }
    }

    private List<Expense> getRecentExpenses(List<Expense> allExpenses, String query) {
        LocalDate cutoffDate;
        if (query.contains("today")) {
            cutoffDate = LocalDate.now();
        } else if (query.contains("week")) {
            cutoffDate = LocalDate.now().minusWeeks(1);
        } else if (query.contains("month")) {
            cutoffDate = LocalDate.now().minusMonths(1);
        } else {
            cutoffDate = LocalDate.now().minusWeeks(2); // Default to 2 weeks
        }

        return allExpenses.stream()
            .filter(e -> {
                try {
                    return LocalDate.parse(e.getDate()).isAfter(cutoffDate) || 
                           LocalDate.parse(e.getDate()).isEqual(cutoffDate);
                } catch (DateTimeParseException ex) {
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    private List<Expense> getCategorySpecificExpenses(List<Expense> allExpenses, String query) {
        // Extract category from query
        String[] categories = {"food", "grocery", "transport", "entertainment", "utilities", "shopping", "health"};
        
        for (String category : categories) {
            if (query.contains(category)) {
                return allExpenses.stream()
                    .filter(e -> e.getCategory().toLowerCase().contains(category))
                    .collect(Collectors.toList());
            }
        }
        
        // If no specific category found, return all expenses limited to 100
        return allExpenses.stream().limit(100).collect(Collectors.toList());
    }

    private List<Expense> getTrendAnalysisExpenses(List<Expense> allExpenses) {
        // For trend analysis, get last 3 months of data
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        
        return allExpenses.stream()
            .filter(e -> {
                try {
                    return LocalDate.parse(e.getDate()).isAfter(threeMonthsAgo);
                } catch (DateTimeParseException ex) {
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    private List<Expense> getBudgetRelevantExpenses(List<Expense> allExpenses) {
        // For budget insights, focus on high-value expenses and recent patterns
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        
        List<Expense> recentExpenses = allExpenses.stream()
            .filter(e -> {
                try {
                    return LocalDate.parse(e.getDate()).isAfter(oneMonthAgo);
                } catch (DateTimeParseException ex) {
                    return false;
                }
            })
            .collect(Collectors.toList());

        // Also include top expenses for context (merge with recent)
        Map<Long, Expense> combinedExpenses = new HashMap<>();
        
        // Add recent expenses
        recentExpenses.forEach(e -> combinedExpenses.put(e.getId(), e));
        
        // Add top expenses
        allExpenses.stream()
            .sorted((a, b) -> Double.compare(b.getAmount(), a.getAmount()))
            .limit(20)
            .forEach(e -> combinedExpenses.put(e.getId(), e));

        return new ArrayList<>(combinedExpenses.values());
    }

    // Create optimized prompt with structured data
    private String createSmartPrompt(List<Expense> expenses, String userQuery, QueryType queryType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze my expense data and answer: ").append(userQuery).append("\n\n");
        
        if (expenses.isEmpty()) {
            prompt.append("No expense data available for the requested criteria.");
            return prompt.toString();
        }

        // Add summary statistics
        double totalAmount = expenses.stream().mapToDouble(Expense::getAmount).sum();
        long totalCount = expenses.size();
        double avgAmount = totalAmount / totalCount;
        
        prompt.append("EXPENSE SUMMARY:\n");
        prompt.append(String.format("Total: $%.2f | Count: %d | Average: $%.2f\n", 
                     totalAmount, totalCount, avgAmount));
        
        // Category breakdown
        Map<String, Double> categoryTotals = expenses.stream()
            .collect(Collectors.groupingBy(
                Expense::getCategory,
                Collectors.summingDouble(Expense::getAmount)
            ));
        
        prompt.append("\nCATEGORY BREAKDOWN:\n");
        categoryTotals.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10) // Top 10 categories
            .forEach(entry -> {
                double percentage = (entry.getValue() / totalAmount) * 100;
                prompt.append(String.format("- %s: $%.2f (%.1f%%)\n", 
                             entry.getKey(), entry.getValue(), percentage));
            });
        
        // Add monthly trends for trend analysis
        if (queryType == QueryType.SPENDING_TRENDS || queryType == QueryType.GENERAL) {
            Map<String, Double> monthlyTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                    expense -> {
                        try {
                            return LocalDate.parse(expense.getDate()).toString().substring(0, 7); // YYYY-MM
                        } catch (DateTimeParseException e) {
                            return expense.getDate().length() >= 7 ? expense.getDate().substring(0, 7) : "Unknown";
                        }
                    },
                    Collectors.summingDouble(Expense::getAmount)
                ));
            
            if (monthlyTotals.size() > 1) {
                prompt.append("\nMONTHLY TRENDS:\n");
                monthlyTotals.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> prompt.append(String.format("- %s: $%.2f\n", 
                                     entry.getKey(), entry.getValue())));
            }
        }
        
        // Include detailed transactions only if manageable count
        if (expenses.size() <= 20) {
            prompt.append("\nRECENT TRANSACTIONS:\n");
            expenses.stream()
                .sorted((a, b) -> {
                    try {
                        return LocalDate.parse(b.getDate()).compareTo(LocalDate.parse(a.getDate()));
                    } catch (DateTimeParseException e) {
                        return b.getDate().compareTo(a.getDate());
                    }
                })
                .limit(15)
                .forEach(expense -> 
                    prompt.append(String.format("- %s: $%.2f (%s) [%s]\n", 
                        expense.getDescription().length() > 30 ? 
                            expense.getDescription().substring(0, 30) + "..." : expense.getDescription(),
                        expense.getAmount(), 
                        expense.getCategory(), 
                        expense.getDate()))
                );
        } else {
            // For large datasets, show top expenses
            prompt.append("\nTOP EXPENSES:\n");
            expenses.stream()
                .sorted((a, b) -> Double.compare(b.getAmount(), a.getAmount()))
                .limit(10)
                .forEach(expense -> 
                    prompt.append(String.format("- %s: $%.2f (%s) [%s]\n", 
                        expense.getDescription().length() > 30 ? 
                            expense.getDescription().substring(0, 30) + "..." : expense.getDescription(),
                        expense.getAmount(), 
                        expense.getCategory(), 
                        expense.getDate()))
                );
        }
        
        // Add specific guidance based on query type
        prompt.append("\nPlease provide specific insights and actionable recommendations.");
        
        // Limit total prompt length to avoid API issues
        String finalPrompt = prompt.toString();
        if (finalPrompt.length() > 3000) {
            finalPrompt = finalPrompt.substring(0, 3000) + "... [truncated]";
        }
        
        return finalPrompt;
    }
}