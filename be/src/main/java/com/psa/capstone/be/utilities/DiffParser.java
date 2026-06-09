package com.psa.capstone.be.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class DiffParser {
    public static final Logger logger = LoggerFactory.getLogger(DiffParser.class);
    public static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("@@ -(\\d+),\\d+ \\+(\\d+),\\d+ @@");

    public String buildFormattedDiff(JsonNode mergeRequestChanges) {
        StringBuilder diffBuilder = new StringBuilder();
        JsonNode changes = mergeRequestChanges.get("changes");

        for (JsonNode change : changes) {
            String filePath = change.get("new_path").asText();
            String rawDiff = change.get("diff").asText();

            // Skip binary files which have a special difference indicator
            if (rawDiff.contains("Binary files") ||
                    rawDiff.contains("diff --git") && !rawDiff.contains("@@ ")) {
                logger.info("Skipping binary file: {}", filePath);
                continue;
            }

            diffBuilder.append("File: ").append(filePath).append("\n\n");

            // Parse the diff into hunks
            List<DiffHunk> hunks = parseFileDiff(rawDiff);

            // Format each hunk with line numbers and old/new separation
            for (DiffHunk hunk : hunks) {
                diffBuilder.append(formatDiffHunk(hunk)).append("\n\n");
            }
        }

        return diffBuilder.toString();
    }

    public List<DiffHunk> parseFileDiff(String rawDiff) {
        List<DiffHunk> hunks = new ArrayList<>();
        String[] lines = rawDiff.split("\n");

        // Quickly return if this is a binary file
        if (rawDiff.contains("Binary files") ||
                (rawDiff.contains("diff --git") && !rawDiff.contains("@@ "))) {
            return hunks; // Return empty list for binary files
        }

        StringBuilder currentHunk = new StringBuilder();
        int currentStartLine = 0;
        int currentLineCount = 0;
        List<String> currentComments = new ArrayList<>();

        for (String line : lines) {
            Matcher matcher = HUNK_HEADER_PATTERN.matcher(line);

            if (matcher.find()) {
                // If we have a previous hunk, save it
                if (currentHunk.length() > 0) {
                    hunks.add(new DiffHunk(
                            currentStartLine,
                            currentStartLine + currentLineCount - 1,
                            currentHunk.toString(),
                            new ArrayList<>(currentComments)));
                }

                // Start new hunk
                currentStartLine = Integer.parseInt(matcher.group(2)); // Use the new file line number
                currentLineCount = 0;
                currentHunk = new StringBuilder();
                currentComments.clear();
                currentHunk.append(line).append("\n");
            } else {
                currentHunk.append(line).append("\n");
                if (!line.startsWith("-")) { // Count lines that aren't removals
                    currentLineCount++;
                }
            }
        }

        // Add the last hunk
        if (currentHunk.length() > 0) {
            hunks.add(new DiffHunk(
                    currentStartLine,
                    currentStartLine + currentLineCount - 1,
                    currentHunk.toString(),
                    currentComments));
        }

        return hunks;
    }

    public String formatDiffHunk(DiffHunk hunk) {
        String[] lines = hunk.content.split("\n");
        StringBuilder formatted = new StringBuilder();

        // Split into old and new sections
        StringBuilder oldHunk = new StringBuilder();
        StringBuilder newHunk = new StringBuilder();
        int currentLine = hunk.startLine;

        for (String line : lines) {
            if (line.startsWith("@@")) {
                // Hunk header goes in both
                oldHunk.append(line).append("\n");
                newHunk.append(line).append("\n");
            } else if (line.startsWith("-")) {
                // Removed lines go in old hunk
                oldHunk.append(line).append("\n");
            } else if (line.startsWith("+")) {
                // Added lines go in new hunk with line numbers
                newHunk.append(String.format("%4d: %s\n", currentLine++, line));
            } else {
                // Context lines go in both, but only new gets line numbers
                oldHunk.append(line).append("\n");
                newHunk.append(String.format("%4d: %s\n", currentLine++, line));
            }
        }

        // Combine with clear separation
        formatted.append("__old hunk__\n")
                .append(oldHunk)
                .append("\n__new hunk__\n")
                .append(newHunk);

        return formatted.toString();
    }
}