// package com.psa.capstone.be.service;

// import org.springframework.ai.embedding.EmbeddingModel;
// import org.springframework.ai.chroma.vectorstore.ChromaApi;
// import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
// import org.springframework.ai.document.Document;
// import org.springframework.ai.vectorstore.SearchRequest;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;
// import com.fasterxml.jackson.databind.JsonNode;
// import org.springframework.ai.vectorstore.VectorStore;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.stream.Collectors;

// @Service
// public class FeedbackFilterService {
// private static final Logger logger =
// LoggerFactory.getLogger(FeedbackFilterService.class);
// private static final double SIMILARITY_THRESHOLD = 0.85;
// private static final int MIN_SIMILAR_COMMENTS = 3;

// @Autowired
// VectorStore vectorStore;

// private final WebClient gitlabWebClient;
// private final EmbeddingModel embeddingModel;

// public FeedbackFilterService(WebClient gitlabWebClient, EmbeddingModel
// embeddingModel) {
// this.gitlabWebClient = gitlabWebClient;
// this.embeddingModel = embeddingModel;
// }

// private record ProcessedComment(String text, int thumbsUp, int thumbsDown) {
// }

// public void processMergedRequestFeedback(Long projectId, Long
// mergeRequestIid) {
// try {
// logger.info("Processing feedback for merged MR#{} in project {}",
// mergeRequestIid, projectId);

// // Fetch all comments for the merge request
// JsonNode comments = gitlabWebClient.get()
// .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}/notes",
// projectId, mergeRequestIid)
// .retrieve()
// .bodyToMono(JsonNode.class)
// .block();

// if (comments == null || !comments.isArray()) {
// logger.warn("No comments found for MR#{}", mergeRequestIid);
// return;
// }

// List<ProcessedComment> approvedComments = new ArrayList<>();
// List<ProcessedComment> rejectedComments = new ArrayList<>();

// for (JsonNode comment : comments) {
// if (!comment.has("body") || !comment.has("id")) {
// continue;
// }

// String commentText = comment.get("body").asText();
// Long noteId = comment.get("id").asLong();

// // Only process comments that appear to be from the AI code review
// if (!isAiReviewComment(commentText)) {
// continue;
// }

// // Extract and clean the feedback text
// String feedbackText = extractFeedbackText(commentText);
// if (feedbackText.isEmpty()) {
// continue;
// }

// // Fetch emoji reactions for this comment
// JsonNode reactions = gitlabWebClient.get()
// .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}/notes/{noteId}/award_emoji",
// projectId, mergeRequestIid, noteId)
// .retrieve()
// .bodyToMono(JsonNode.class)
// .block();

// int thumbsUp = 0;
// int thumbsDown = 0;

// if (reactions != null && reactions.isArray()) {
// for (JsonNode reaction : reactions) {
// if (reaction.has("name")) {
// String emojiName = reaction.get("name").asText();
// if ("thumbsup".equals(emojiName)) {
// thumbsUp++;
// } else if ("thumbsdown".equals(emojiName)) {
// thumbsDown++;
// }
// }
// }
// }

// if (thumbsUp == 0 && thumbsDown == 0) {
// continue;
// }

// ProcessedComment processedComment = new ProcessedComment(
// feedbackText,
// thumbsUp,
// thumbsDown);

// if (thumbsUp > thumbsDown) {
// approvedComments.add(processedComment);
// storeComment(feedbackText, "upvoted", projectId, mergeRequestIid);
// } else {
// rejectedComments.add(processedComment);
// storeComment(feedbackText, "downvoted", projectId, mergeRequestIid);
// }
// }

// // Generate and post summary
// postFeedbackSummary(projectId, mergeRequestIid, approvedComments,
// rejectedComments);

// } catch (Exception e) {
// logger.error("Error processing feedback for MR#{}: {}", mergeRequestIid,
// e.getMessage(), e);
// }
// }

// boolean isAiReviewComment(String commentText) {
// // Look for markers that identify AI review comments
// return commentText.contains("🤖");
// }

// private String extractFeedbackText(String commentText) {
// return commentText
// .replaceAll("^### 🤖.*?\n\n", "") // Remove the markdown header
// .replaceAll("```[^`]*```", "") // Remove any code blocks
// .trim();
// }

// private void postFeedbackSummary(Long projectId, Long mergeRequestIid,
// List<ProcessedComment> approvedComments,
// List<ProcessedComment> rejectedComments) {
// StringBuilder summary = new StringBuilder();
// summary.append("## 📊 Code Review Feedback Summary\n\n");

// summary.append("### 👍 Approved Comments
// (").append(approvedComments.size()).append(")\n");
// for (ProcessedComment comment : approvedComments) {
// summary.append("- ").append(comment.text) // Use full comment text
// .append(" (👍 ").append(comment.thumbsUp)
// .append(" | 👎 ").append(comment.thumbsDown).append(")\n\n");
// }

// summary.append("\n### 👎 Rejected Comments
// (").append(rejectedComments.size()).append(")\n");
// for (ProcessedComment comment : rejectedComments) {
// summary.append("- ").append(comment.text) // Use full comment text
// .append(" (👍 ").append(comment.thumbsUp)
// .append(" | 👎 ").append(comment.thumbsDown).append(")\n\n");
// }

// // Post the summary as a comment
// try {
// gitlabWebClient.post()
// .uri("/projects/{projectId}/merge_requests/{mergeRequestIid}/notes",
// projectId, mergeRequestIid)
// .bodyValue(Map.of("body", summary.toString()))
// .retrieve()
// .bodyToMono(JsonNode.class)
// .block();

// logger.info("Successfully posted feedback summary for MR#{}",
// mergeRequestIid);
// } catch (Exception e) {
// logger.error("Failed to post feedback summary for MR#{}: {}",
// mergeRequestIid, e.getMessage(),
// e);
// }
// }

// // Search for similar comments in vector store but are downvoted
// public boolean shouldBlockComment(String proposedComment) {
// List<Document> similarDownvotedComments = vectorStore.similaritySearch(
// SearchRequest.builder()
// .query(proposedComment)
// .filterExpression("feedback_type == 'downvoted'")
// .similarityThreshold(SIMILARITY_THRESHOLD)
// .build());
// // Do not show this comment to user if there are at least 3 similar downvoted
// // ones
// logger.info("Found {} similar downvoted comments",
// similarDownvotedComments.size());
// return similarDownvotedComments.size() >= MIN_SIMILAR_COMMENTS;
// }

// private void storeComment(String commentText, String feedbackType, Long
// projectId, Long mrIid) {

// logger.info("storing comment: {} feedback is {}", commentText, feedbackType);

// Document document = new Document(
// commentText,
// Map.of(
// "feedback_type", feedbackType,
// "project_id", projectId.toString(),
// "merge_request_iid", mrIid.toString(),
// "stored_at", System.currentTimeMillis()));

// vectorStore.add(List.of(document));
// }
// }
