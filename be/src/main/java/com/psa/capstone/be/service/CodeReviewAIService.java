package com.psa.capstone.be.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psa.capstone.be.dto.CodeReviewDTO;
import com.psa.capstone.be.entity.CustomRuleEntity;
import com.psa.capstone.be.exception.GitLabException;
import com.psa.capstone.be.utilities.DiffParser;
import com.psa.capstone.be.utilities.LLMPrompt;

@Service
public class CodeReviewAIService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewAIService.class);
    private static final int MAX_TOKENS = 30000; // Maximum tokens for LLM context

    private final OllamaChatModel chatModel;
    private final WebClient gitlabWebClient;
    private final DiffParser diffParser;
    private final BeanOutputConverter<CodeReviewDTO> outputConverter;
    // private final FeedbackFilterService feedbackFilterService;
    private final CustomRuleService customRuleService;
    private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    private record ProjectInfo(String name, Long parentGroupId) {
    }

    private record DiffRefs(String baseSha, String startSha, String headSha) {
    }

    public CodeReviewAIService(
            OllamaChatModel chatModel,
            WebClient gitlabWebClient,
            DiffParser diffParser,
            // FeedbackFilterService feedbackFilterService,
            CustomRuleService customRuleService) {
        this.chatModel = chatModel;
        this.gitlabWebClient = gitlabWebClient;
        this.diffParser = diffParser;
        this.outputConverter = new BeanOutputConverter<>(CodeReviewDTO.class);
        // this.feedbackFilterService = feedbackFilterService;
        this.customRuleService = customRuleService;
    }

    @Async
    public void reviewMergeRequest(Long projectId, String sourceBranch, String targetBranch, Long mergeRequestIid) {

        logger.info("Starting code review for MR#{} - Project: {}, Source: {}, Target: {}",
                mergeRequestIid, projectId, sourceBranch, targetBranch);

        try {

            // Fetch the parent group ID for this project
            ProjectInfo projectInfo = fetchProjectInfo(projectId);

            if (projectInfo == null) {
                logger.error("Failed to fetch project information for project ID: {}", projectId);
                postComment(projectId, mergeRequestIid, "Error: Could not retrieve project information");
                return;
            }

            logger.info("Project name: {}, Parent group ID: {}", projectInfo.name(), projectInfo.parentGroupId());

            // Fetch changes directly from merge request
            JsonNode mergeRequestChanges = gitlabWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/{projectId}/merge_requests/{mergeRequestIid}/changes")
                            .build(projectId, mergeRequestIid))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String formattedDiff = diffParser.buildFormattedDiff(mergeRequestChanges);

            logger.info("Formatted diff for MR#{}: \n{}", mergeRequestIid,
                    formattedDiff);

            if (mergeRequestChanges == null || !mergeRequestChanges.has("changes") ||
                    mergeRequestChanges.get("changes").isEmpty()) {
                postComment(projectId, mergeRequestIid, "No changes found to review.");
                return;
            }

            // Generate the review using AI
            CodeReviewDTO review = generateReview(sourceBranch, targetBranch,
                    formattedDiff, projectInfo.parentGroupId(), projectInfo.name());

            // Post the review as comments
            postReviewComments(projectId, mergeRequestIid, review);

        } catch (Exception e) {
            logger.error("Error during code review for MR#{}: {}", mergeRequestIid, e.getMessage(), e);
            String errorMessage = String.format("⚠️ Error during code review: %s", e.getMessage());
            try {
                postComment(projectId, mergeRequestIid, errorMessage);
            } catch (Exception commentError) {
                logger.error("Failed to post error comment: {}", commentError.getMessage(), commentError);
            }
        }
    }

    private ProjectInfo fetchProjectInfo(Long projectId) {
        try {
            JsonNode projectInfo = gitlabWebClient.get()
                    .uri("/projects/{projectId}", projectId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (projectInfo == null) {
                logger.error("Received null response for project ID: {}", projectId);
                return null;
            }

            String name = null;
            Long parentGroupId = null;

            // Extract project name
            if (projectInfo.has("name")) {
                name = projectInfo.get("name").asText();
            }

            // Extract parent group ID from namespace
            if (projectInfo.has("namespace")) {
                JsonNode namespace = projectInfo.get("namespace");

                if (namespace.has("parent_id")) {
                    parentGroupId = namespace.get("parent_id").asLong();
                } else if (namespace.has("id")) {
                    parentGroupId = namespace.get("id").asLong();
                }
            }

            logger.info("Retrieved project info - Name: {}, Parent Group ID: {}", name, parentGroupId);
            return new ProjectInfo(name, parentGroupId);

        } catch (Exception e) {
            logger.error("Error fetching project info: {}", e.getMessage(), e);
            return null;
        }
    }

    private CodeReviewDTO generateReview(String sourceBranch, String targetBranch, String codeDiff,
            Long parentGroupId, String projectName) {
        try {
            // Fetch custom rules for the parent group
            String customRules = getCustomRulesForPrompt(parentGroupId, projectName);

            // Check if we need to chunk the diff (estimate tokens in prompt)
            String promptTemplate = LLMPrompt.REVIEW_PROMPT_TEMPLATE
                    .replace("{sourceBranch}", sourceBranch)
                    .replace("{targetBranch}", targetBranch)
                    .replace("{customRules}", customRules)
                    .replace("{format}", outputConverter.getFormat());

            int basePromptTokens = estimateTokens(promptTemplate);
            int diffTokens = estimateTokens(codeDiff);

            logger.info("Prompt base tokens: {}, Diff tokens: {}, Total: {}, Max: {}",
                    basePromptTokens, diffTokens, basePromptTokens + diffTokens, MAX_TOKENS);

            if (basePromptTokens + diffTokens > MAX_TOKENS) {
                logger.info("Diff is too large for a single prompt. Using chunking approach.");
                return processLargeCodeDiff(sourceBranch, targetBranch, codeDiff, customRules);
            } else {
                return processSingleChunk(sourceBranch, targetBranch, codeDiff, customRules);
            }
        } catch (Exception e) {
            logger.error("Error generating review: {}", e.getMessage(), e);
            throw new GitLabException("Failed to generate code review", e);
        }
    }

    // Process a single chunk (existing logic)
    private CodeReviewDTO processSingleChunk(String sourceBranch, String targetBranch, String codeDiff,
            String customRules) {
        Map<String, Object> parameters = Map.of(
                "sourceBranch", sourceBranch,
                "targetBranch", targetBranch,
                "codeDiff", codeDiff,
                "customRules", customRules,
                "format", outputConverter.getFormat());

        PromptTemplate template = new PromptTemplate(LLMPrompt.REVIEW_PROMPT_TEMPLATE);
        Message message = new UserMessage(template.render(parameters));
        Prompt prompt = new Prompt(message);

        logger.info("the prompt is {}", prompt);

        ChatResponse response = chatModel.call(prompt);
        String output = response.getResult().getOutput().getContent();

        // Remove markdown code block markers and trim
        output = output.replace("```json", "").replace("```", "").trim();

        // Clean up JSON issues
        output = cleanupJson(output);

        // Validate JSON structure before conversion
        validateJson(output);

        return outputConverter.convert(output);
    }

    // New method to handle large diffs by chunking
    private CodeReviewDTO processLargeCodeDiff(String sourceBranch, String targetBranch, String codeDiff,
            String customRules) {
        // Split the diff by files
        List<String> fileChunks = splitDiffByFile(codeDiff);
        logger.info("Split diff into {} file chunks", fileChunks.size());

        List<CodeReviewDTO> chunkReviews = new ArrayList<>();
        int availableTokens = MAX_TOKENS - estimateTokens(LLMPrompt.REVIEW_PROMPT_TEMPLATE
                .replace("{sourceBranch}", sourceBranch)
                .replace("{targetBranch}", targetBranch)
                .replace("{customRules}", customRules)
                .replace("{format}", outputConverter.getFormat()));

        logger.info("Available tokens for diff content: {}", availableTokens);

        StringBuilder currentChunk = new StringBuilder();

        // Process files one by one, combining them into chunks when possible
        for (String fileChunk : fileChunks) {
            int fileTokens = estimateTokens(fileChunk);
            logger.info("File chunk size: {} tokens", fileTokens);

            // If a single file is too large, we need to split it further
            if (fileTokens > availableTokens) {
                logger.info("Single file chunk too large, splitting further");
                List<String> subChunks = handleLargeFile(fileChunk, availableTokens);

                // Process current accumulated chunk if not empty
                if (currentChunk.length() > 0) {
                    logger.info("Processing accumulated chunk before large file");
                    CodeReviewDTO review = processSingleChunk(
                            sourceBranch, targetBranch, currentChunk.toString(), customRules);
                    chunkReviews.add(review);
                    currentChunk = new StringBuilder();
                }

                // Process each sub-chunk of the large file
                for (String subChunk : subChunks) {
                    logger.info("Processing sub-chunk of large file");
                    CodeReviewDTO review = processSingleChunk(
                            sourceBranch, targetBranch, subChunk, customRules);
                    chunkReviews.add(review);
                }
                continue;
            }

            // Check if adding this file to the current chunk would exceed our limit
            if (currentChunk.length() > 0 &&
                    estimateTokens(currentChunk.toString() + fileChunk) > availableTokens) {
                // Process the current chunk
                logger.info("Processing chunk of accumulated files");
                CodeReviewDTO review = processSingleChunk(
                        sourceBranch, targetBranch, currentChunk.toString(), customRules);
                chunkReviews.add(review);
                currentChunk = new StringBuilder(fileChunk);
            } else {
                // Add to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(fileChunk);
            }
        }

        // Process the final chunk if it's not empty
        if (currentChunk.length() > 0) {
            logger.info("Processing final accumulated chunk");
            CodeReviewDTO review = processSingleChunk(
                    sourceBranch, targetBranch, currentChunk.toString(), customRules);
            chunkReviews.add(review);
        }

        logger.info("Combining {} chunk reviews", chunkReviews.size());
        // Combine all results into a single review
        return combineReviews(chunkReviews);
    }

    // Split the diff by file
    private List<String> splitDiffByFile(String diff) {
        List<String> fileChunks = new ArrayList<>();
        String[] lines = diff.split("\n");
        StringBuilder currentFile = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("File: ") && currentFile.length() > 0) {
                fileChunks.add(currentFile.toString());
                currentFile = new StringBuilder();
            }
            currentFile.append(line).append("\n");
        }

        if (currentFile.length() > 0) {
            fileChunks.add(currentFile.toString());
        }

        return fileChunks;
    }

    // Handle a file that's too large for a single chunk
    private List<String> handleLargeFile(String fileContent, int maxTokens) {
        List<String> chunks = new ArrayList<>();

        // If the file is too large, use a token-based splitter
        if (estimateTokens(fileContent) > maxTokens) {
            // Extract file path for preserving context
            String filePath = "";
            String[] lines = fileContent.split("\n");
            if (lines.length > 0 && lines[0].startsWith("File: ")) {
                filePath = lines[0] + "\n\n";
            }

            // Create a smaller chunk size to ensure each chunk fits
            int chunkSize = (int) (maxTokens * 0.8); // 80% of max to allow for overhead

            List<Document> documents = new TokenTextSplitter(chunkSize, 200, 0, 10000, true)
                    .apply(List.of(new Document(fileContent)));

            for (Document doc : documents) {
                // Prepend file path to each chunk for context
                if (!filePath.isEmpty() && !doc.getContent().startsWith("File: ")) {
                    chunks.add(filePath + doc.getContent());
                } else {
                    chunks.add(doc.getContent());
                }
            }
        } else {
            chunks.add(fileContent);
        }

        return chunks;
    }

    // Combine multiple reviews into a single review
    private CodeReviewDTO combineReviews(List<CodeReviewDTO> reviews) {
        if (reviews.size() == 1) {
            return reviews.get(0);
        }

        // Combine summaries
        StringBuilder combinedSummary = new StringBuilder(
                "This code review was processed in multiple parts due to its size. Combined summary:\n\n");
        int partNumber = 1;
        for (CodeReviewDTO review : reviews) {
            combinedSummary.append("Part ").append(partNumber++).append(": ")
                    .append(review.summary()).append("\n\n");
        }

        // Combine all issues
        List<CodeReviewDTO.CodeIssue> allIssues = new ArrayList<>();
        for (CodeReviewDTO review : reviews) {
            allIssues.addAll(review.issues());
        }

        // Create combined review
        return new CodeReviewDTO(
                combinedSummary.toString(),
                allIssues.size(),
                allIssues);
    }

    // Validate the JSON
    private void validateJson(String output) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(output);
            logger.debug("Generated valid JSON response: {}", output);
        } catch (Exception e) {
            logger.error("Invalid JSON response from LLM: {}", output);
            throw new GitLabException("AI generated an invalid JSON response", e);
        }
    }

    // Estimate tokens in a string
    private int estimateTokens(String text) {
        return tokenCountEstimator.estimate(text);
    }

    private String getCustomRulesForPrompt(Long parentGroupId, String projectName) {
        List<CustomRuleEntity> customRules = List.of();

        if (parentGroupId != null) {
            // Try to determine category from project name
            CustomRuleEntity.RuleCategory category = getCategoryFromProjectName(projectName);

            if (category != null) {
                // Get rules specific to the parent group and category
                logger.info("Applying rules for category: {}", category);
                customRules = customRuleService.getEnabledRulesByParentGroupAndCategory(parentGroupId, category);
            } else {
                // If category can't be determined, get all rules for this group
                logger.info("Could not determine category from project name: {}, applying all rules", projectName);
                customRules = customRuleService.getEnabledRulesByParentGroup(parentGroupId);
            }
        }

        if (customRules.isEmpty()) {
            return ""; // No custom rules to apply
        }

        // Format the rules for the prompt with clearer structure
        StringBuilder rulesBuilder = new StringBuilder();

        for (int i = 0; i < customRules.size(); i++) {
            CustomRuleEntity rule = customRules.get(i);
            String severity = rule.getSeverity().toString();

            // Rule header with number and severity
            rulesBuilder.append(String.format("RULE #%d [%s]: %s\n",
                    i + 1, severity, rule.getDescription()));

            // Add remark if it exists with clear separator
            if (rule.getRemark() != null && !rule.getRemark().isEmpty()) {
                rulesBuilder.append("EXAMPLE OF VIOLATION:\n```\n")
                        .append(rule.getRemark().trim())
                        .append("\n```\n");
            }

            // Add separator between rules
            if (i < customRules.size() - 1) {
                rulesBuilder.append("---\n");
            }
        }

        return rulesBuilder.toString();
    }

    private CustomRuleEntity.RuleCategory getCategoryFromProjectName(String projectName) {
        if (projectName == null) {
            return null;
        }

        // Convert to lowercase for case-insensitive matching
        String lowerName = projectName.toLowerCase();

        // Check for fe pattern (fe, fe2, fe-something, etc.)
        if (lowerName.startsWith("fe") || lowerName.contains("-fe")) {
            return CustomRuleEntity.RuleCategory.FE;
        }

        // Check for be pattern
        if (lowerName.startsWith("be") || lowerName.contains("-be")) {
            return CustomRuleEntity.RuleCategory.BE;
        }

        // Check for svc pattern
        if (lowerName.startsWith("svc") || lowerName.contains("-svc")) {
            return CustomRuleEntity.RuleCategory.SVC;
        }

        // If no match, return null
        return null;
    }

    private String cleanupJson(String json) {
        // Remove any trailing commas before closing brackets or braces
        json = json.replaceAll(",\\s*([}\\]])", "$1");

        // Fix any potential duplicate commas
        json = json.replaceAll(",\\s*,", ",");

        // Remove any potential control characters
        json = json.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return json;
    }

    private void postComment(Long projectId, Long mergeRequestIid, String body) {
        try {
            gitlabWebClient.post()
                    .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}/notes",
                            projectId, mergeRequestIid)
                    .bodyValue(Map.of("body", body))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to post comment: {}", e.getMessage(), e);
            throw new GitLabException("Failed to post review comment", e);
        }
    }

    private Long createDiscussionOnLine(
            Long projectId,
            Long mergeRequestIid,
            String filePath,
            int fromLine,
            int toLine,
            String comment,
            String suggestedCode,
            DiffRefs diffRefs) {
        try {

            // Build the position object for the specific line
            Map<String, Object> position = new HashMap<>();
            position.put("base_sha", diffRefs.baseSha());
            position.put("start_sha", diffRefs.startSha());
            position.put("head_sha", diffRefs.headSha());
            position.put("position_type", "text");
            position.put("new_path", filePath);
            position.put("old_path", filePath);
            position.put("new_line", toLine);

            logger.info("range from {} to {}", fromLine, toLine);

            // Create comment body with suggestion if code is provided
            String body = comment;

            if (suggestedCode != null && !suggestedCode.isEmpty()) {
                body += "\n\n```\n" + suggestedCode + "\n```";
            }

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("body", body);
            requestBody.put("position", position);

            logger.info("Creating discussion with body: {}", requestBody);

            // Create the discussion
            JsonNode response = gitlabWebClient.post()
                    .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}/discussions",
                            projectId, mergeRequestIid)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("id")) {
                return response.get("id").asLong();
            }

            logger.warn("Failed to extract discussion ID from response: {}", response);
            return null;

        } catch (Exception e) {
            logger.error("Failed to create discussion: {}", e.getMessage(), e);
            return null;
        }
    }

    private void postReviewComments(Long projectId, Long mergeRequestIid, CodeReviewDTO review) {
        DiffRefs diffRefs = fetchMergeRequestRefs(projectId, mergeRequestIid);

        if (diffRefs == null) {
            logger.error("Failed to fetch diff refs for MR#{}", mergeRequestIid);
            return;
        }

        // Post summary comment first
        StringBuilder summaryComment = new StringBuilder();
        summaryComment.append("## 🤖 AI Code Review Summary\n\n");
        summaryComment.append(review.summary()).append("\n\n");

        // Track processed vs filtered comments
        int totalComments = review.issues().size();
        int filteredComments = 0;

        // Post individual issues as line discussions
        for (CodeReviewDTO.CodeIssue issue : review.issues()) {
            try {
                String comment = String.format("### 🤖 %s\n\n%s", issue.title(), issue.explanation());

                // Check if comment should be blocked based on previous feedback
                // if (feedbackFilterService.shouldBlockComment(comment)) {
                // logger.info("Skipping comment due to negative feedback history: {}",
                // issue.title());
                // filteredComments++;
                // continue;
                // }

                Long discussionId = createDiscussionOnLine(
                        projectId,
                        mergeRequestIid,
                        issue.filePath(),
                        issue.lineRange().fromLine(),
                        issue.lineRange().toLine(),
                        comment,
                        issue.suggestedCode(),
                        diffRefs);

                if (discussionId != null) {
                    logger.info("Successfully created discussion ID: {} for issue: {}",
                            discussionId, issue.title());
                }
            } catch (Exception e) {
                logger.error("Error creating discussion for issue {}: {}", issue.title(), e.getMessage());
            }
        }

        // Add filtering stats to summary
        summaryComment.append("**Total issues found:** ").append(totalComments).append("\n");
        if (filteredComments > 0) {
            summaryComment.append("**Comments filtered:** ").append(filteredComments)
                    .append(" (based on previous feedback)\n");
        }

        postComment(projectId, mergeRequestIid, summaryComment.toString());
    }

    private DiffRefs fetchMergeRequestRefs(Long projectId, Long mergeRequestIid) {
        try {
            JsonNode mrDetails = gitlabWebClient.get()
                    .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}",
                            projectId, mergeRequestIid)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (mrDetails == null || !mrDetails.has("diff_refs")) {
                logger.error("Failed to fetch merge request details or diff_refs not found");
                return null;
            }

            JsonNode diffRefs = mrDetails.get("diff_refs");
            return new DiffRefs(
                    diffRefs.get("base_sha").asText(),
                    diffRefs.get("start_sha").asText(),
                    diffRefs.get("head_sha").asText());
        } catch (Exception e) {
            logger.error("Error fetching merge request refs: {}", e.getMessage(), e);
            return null;
        }
    }
}