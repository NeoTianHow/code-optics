package com.psa.capstone.be.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.psa.capstone.be.exception.BranchComparisonException;

@Service
public class BranchComparisonAIService {
  @Autowired
  OllamaChatModel chatModel;

  private static final Logger logger = LoggerFactory.getLogger(BranchComparisonAIService.class);
  private static final int MAX_TOKENS = 30000;
  private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

  private static final String DETAILED_PROMPT_TEMPLATE = """
      You are Qwen, created by Alibaba Cloud. You are a helpful assistant. You are reviewing a pull request containing changes across multiple files in a software project.
      Your goal is to generate a structured summary that helps developers quickly understand the key modifications. You are given the **code diffs** between two Git branches:
      - **Source Branch**: {source}
      - **Target Branch**: {target}

      The goal is to produce a **clear, concise, and informative PR summary** of the changes.

      First, review these commit messages for context, but DO NOT rely solely on them as they may be brief or incomplete:
      {commitMessages}

      Now, analyze the actual code changes and provide:

      1. **Branch Purpose**:
         - Summarize the overall purpose of the PR in 2 to 3 sentences

      2. **Key Changes**:
         - Group by category (Controllers, Services, etc.)
         - For each file, provide:
           - File name (without the full path)
           - Identify and explain important code changes that is useful for developers.
           - **Exclude** routine version increments (e.g., changing version numbers from 2.2.1 to 2.2.2) and standard procedural updates unless they introduce significant functional changes.

      Note: Lines with '-' are removals, '+' are additions.

      ### Code Changes:
      {codeDiff}
      """;

  private static final String DETAILED_EXAMPLE = """
      ### **Output Example Format** (Generic)

      **Branch Purpose:**
      This branch refactors the authentication module to improve security, simplifies token validation logic, and enhances logging for better debugging. Additionally, it fixes a bug related to session expiration handling.

      **Key Changes:**
      - **Configuration**:
        - File: `SecurityConfig.java` (modified)
          - Added JwtAuthenticationFilter to process and validate JWTs from the Authorization header, enabling role-based access control.

      - **Services**:
        - File: `AuthService.java` (modified)
          - Implemented generateToken() to create JWTs with user-specific claims including user ID and rolles with a 24-hour expiration.
          - Updated authenticateUser() to return JWTs upon successful authentication, replacing server-side sessions.

      - **Controllers**:
        - File: `AuthService.java` (modified)
          - Annotated endpoints to require valid JWTs for user-related functionalities.
      """;

  private static final String CONCISE_SUMMARY_PROMPT = """
      You are Qwen, created by Alibaba Cloud. You are a helpful assistant.

      Your task is to create a brief, focused summary of branch changes from a detailed analysis.
      The input is a detailed summary that includes branch purpose, key changes, and implications.

      Guidelines for the concise summary:
      1. Length: Create a single paragraph of 2-3 sentences
      2. Focus:
         - What was the main purpose of these changes
         - What key functionality was added/modified

      Style Requirements:
      - Use clear, direct language
      - Focus on business/functional impact
      - Omit technical details unless critical
      - Start with "This branch..."
      - Keep total length under 100 words

      Input is a detailed analysis. Convert it to a concise, user-friendly summary.

      Example Output Format:
      "This branch adds user authentication to the platform. It introduces login/logout functionality and secure session management, while also implementing password reset capabilities. These changes improve system security and user account management."

      Now, create a concise summary from this detailed analysis:
      {detailedSummary}
      """;

  public String generateSummaryWithChunking(String codeDiff, String commitMessages, String fromBranch, String toBranch,
      boolean concise) {
    List<String> chunks = chunkDiffByMergingFiles(codeDiff, MAX_TOKENS);
    logger.info("Processing {} chunks", chunks.size());

    if (chunks.size() == 1) {
      return generateSummary(chunks.get(0), commitMessages, fromBranch, toBranch, concise);
    }

    List<String> partialSummaries = chunks.subList(0, chunks.size() - 1).stream()
        .map(chunk -> generateSummary(chunk, commitMessages, fromBranch, toBranch, concise))
        .collect(Collectors.toList());

    return generateFinalSummary(
        String.join("\n\n", partialSummaries),
        chunks.get(chunks.size() - 1),
        commitMessages,
        fromBranch,
        toBranch,
        concise);
  }

  private String generateSummary(String input, String commitMessages, String fromBranch, String toBranch,
      boolean concise) {
    try {
      if (concise) {
        PromptTemplate template = new PromptTemplate(CONCISE_SUMMARY_PROMPT);
        template.add("detailedSummary", input);
        return chatModel.call(template.create())
            .getResult()
            .getOutput()
            .getContent();
      } else {
        PromptTemplate template = new PromptTemplate(DETAILED_PROMPT_TEMPLATE + DETAILED_EXAMPLE);
        template.add("codeDiff", input);
        template.add("commitMessages", commitMessages);
        template.add("source", fromBranch);
        template.add("target", toBranch);

        logger.info("Commit messages: {}", commitMessages);
        return chatModel.call(template.create())
            .getResult()
            .getOutput()
            .getContent();
      }
    } catch (Exception e) {
      logger.error("Error generating summary: {}", e.getMessage(), e);
      throw new BranchComparisonException("Failed to generate summary", e);
    }
  }

  private String generateFinalSummary(String partialSummaries, String lastChunk,
      String commitMessages, String fromBranch, String toBranch, boolean concise) {
    try {
      logger.info("partialSummaries: {}", partialSummaries);
      logger.info("lastChunk: {}", lastChunk);

      String baseTemplate = concise ? CONCISE_SUMMARY_PROMPT
          : """
              You are Qwen, created by Alibaba Cloud. You are a helpful assistant.
              Your task is to merge and organize multiple summaries into a single coherent summary.

              First, review these commit messages for context, but DO NOT rely solely on them as they may be brief or incomplete:
              {commitMessages}

              Please combine the following summaries into one final summary:

              1. Existing Summary:
              {partialSummaries}

              2. New Summary:
              {lastChunk}

              Guidelines:
              - DO NOT reanalyze the changes
              - Simply combine and organize the existing summaries
              - Remove any duplicate information
              - Keep all unique details from both summaries
              - Maintain the same structure and sections as the input
              - Ensure the final summary reads as one cohesive piece
              """;

      PromptTemplate template = new PromptTemplate(baseTemplate);
      template.add("partialSummaries", partialSummaries);
      template.add("lastChunk", lastChunk);
      template.add("commitMessages", commitMessages);

      return chatModel.call(template.create())
          .getResult()
          .getOutput()
          .getContent();
    } catch (Exception e) {
      logger.error("Error generating final summary: {}", e.getMessage(), e);
      throw new BranchComparisonException("Failed to generate final summary", e);
    }
  }

  private List<String> chunkDiffByMergingFiles(String fullDiff, int maxTokens) {
    try {
      List<String> blocks = splitByFile(fullDiff);
      List<String> finalChunks = new ArrayList<>();
      StringBuilder accumulator = new StringBuilder();

      for (String block : blocks) {
        int blockTokens = estimateTokens(block);

        if (blockTokens > maxTokens) {
          if (accumulator.length() > 0) {
            finalChunks.add(accumulator.toString().trim());
            accumulator.setLength(0);
          }
          finalChunks.addAll(tokenSplitFallback(block, maxTokens));
          continue;
        }

        if (accumulator.length() == 0) {
          accumulator.append(block);
        } else {
          String candidate = accumulator + "\n" + block;
          if (estimateTokens(candidate) <= maxTokens) {
            accumulator = new StringBuilder(candidate);
          } else {
            finalChunks.add(accumulator.toString().trim());
            accumulator = new StringBuilder(block);
          }
        }
      }

      if (accumulator.length() > 0) {
        finalChunks.add(accumulator.toString().trim());
      }

      mergeSmallChunks(finalChunks, (int) (0.15 * maxTokens));
      return finalChunks;
    } catch (Exception e) {
      logger.error("Error splitting diff: {}", e.getMessage(), e);
      throw new BranchComparisonException("Failed to process diff chunks", e);
    }
  }

  private List<String> splitByFile(String diff) {
    List<String> blocks = new ArrayList<>();
    StringBuilder currentFile = new StringBuilder();

    for (String line : diff.split("\n")) {
      if (line.startsWith("File: ") && currentFile.length() > 0) {
        blocks.add(currentFile.toString().trim());
        currentFile = new StringBuilder();
      }
      currentFile.append(line).append("\n");
    }

    if (currentFile.length() > 0) {
      blocks.add(currentFile.toString().trim());
    }

    return blocks;
  }

  private void mergeSmallChunks(List<String> chunks, int thresholdTokens) {
    if (chunks.size() < 2)
      return;

    String last = chunks.get(chunks.size() - 1);
    if (estimateTokens(last) < thresholdTokens) {
      String secondLast = chunks.get(chunks.size() - 2);
      String merged = secondLast + "\n" + last;
      if (estimateTokens(merged) <= MAX_TOKENS) {
        chunks.set(chunks.size() - 2, merged);
        chunks.remove(chunks.size() - 1);
      }
    }
  }

  private List<String> tokenSplitFallback(String text, int maxTokens) {
    return new TokenTextSplitter(maxTokens, 350, 5, 10000, true)
        .apply(List.of(new Document(text))).stream()
        .map(Document::getContent)
        .collect(Collectors.toList());
  }

  private int estimateTokens(String text) {
    return tokenCountEstimator.estimate(text);
  }
}