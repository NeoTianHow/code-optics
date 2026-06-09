package com.psa.capstone.be.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({ "summary", "totalIssues", "issues" })
public record CodeReviewDTO(
                @JsonProperty(required = true) String summary,
                @JsonProperty(required = true) int totalIssues,
                @JsonProperty(required = true) List<CodeIssue> issues) {

        @JsonPropertyOrder({ "title", "filePath", "explanation", "lineRange", "suggestedCode" })
        public record CodeIssue(
                        @JsonProperty(required = true) String title,
                        @JsonProperty(required = true) String filePath,
                        @JsonProperty(required = true) String explanation,
                        @JsonProperty(required = true) LineRange lineRange,
                        @JsonProperty(required = true) String suggestedCode) {
        }

        @JsonPropertyOrder({ "fromLine", "toLine" })
        public record LineRange(
                        @JsonProperty(required = true) int fromLine,
                        @JsonProperty(required = true) int toLine) {
        }
}