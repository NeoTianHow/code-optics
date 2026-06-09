package com.psa.capstone.be.controller;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.psa.capstone.be.service.CodeReviewAIService;
// import com.psa.capstone.be.service.FeedbackFilterService;
import com.psa.capstone.be.dto.GitLabWebhookDTO;
import com.psa.capstone.be.exception.InvalidWebhookException;
import com.psa.capstone.be.exception.WebhookProcessingException;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/webhooks")
@Validated
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final CodeReviewAIService codeReviewService;
    // private final FeedbackFilterService feedbackFilterService;

    @Value("${gitlab.webhook.secret}")
    private String webhookSecret;

    // public WebhookController(CodeReviewAIService codeReviewService,
    // FeedbackFilterService feedbackFilterService) {
    // this.codeReviewService = codeReviewService;
    // this.feedbackFilterService = feedbackFilterService;
    // }

    public WebhookController(CodeReviewAIService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/gitlab")
    public ResponseEntity<Void> handleGitLabWebhook(
            @RequestHeader(value = "X-Gitlab-Event", required = true) String eventType,
            @RequestHeader(value = "X-Gitlab-Token", required = true) String token,
            @Valid @RequestBody GitLabWebhookDTO payload) {

        logger.info("Received GitLab webhook: {}", eventType);

        // Verify webhook token
        if (!isValidToken(token)) {
            logger.info("Invalid webhook token received");
            throw new InvalidWebhookException("Invalid webhook token");
        }

        if (!"Merge Request Hook".equals(eventType)) {
            logger.info("Unsupported event type received: {}", eventType);
            throw new InvalidWebhookException("Unsupported event type: " + eventType);
        }

        if (payload == null) {
            throw new InvalidWebhookException("Invalid webhook payload");
        }

        try {
            GitLabWebhookDTO.ObjectAttributes attrs = payload.getObjectAttributes();
            String action = attrs.getAction();

            // // NOTE: CHANGE TO MERGE AFTER DONE TESTING (CHANGE IT TO MERGE)
            // if ("close".equals(action)) {
            // // Process feedback when MR is merged
            // feedbackFilterService.processMergedRequestFeedback(
            // payload.getProject().getId(),
            // attrs.getIid());
            // }

            if ("open".equals(action) || "update".equals(action) ||
                    "reopen".equals(action)) {
                codeReviewService.reviewMergeRequest(
                        payload.getProject().getId(),
                        attrs.getSourceBranch(),
                        attrs.getTargetBranch(),
                        attrs.getIid());
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.info("Error processing webhook: {}", e.getMessage(), e);
            throw new WebhookProcessingException("Error processing webhook", e);
        }
    }

    private boolean isValidToken(String token) {
        return webhookSecret != null && webhookSecret.equals(token);
    }
}