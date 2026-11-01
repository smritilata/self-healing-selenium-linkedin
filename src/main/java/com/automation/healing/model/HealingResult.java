package com.automation.healing.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit record capturing details of a self-healing event.
 */
public class HealingResult implements Serializable {

    private String elementKey;
    private String originalLocator;
    private String healedLocator;
    private double confidenceScore;
    private String strategy; // "FALLBACK_SELECTOR", "SIMILARITY_MATCHING", "STRUCTURAL_HEURISTIC"
    private String timestamp;
    private String screenshotPath;
    private boolean successful;

    public HealingResult() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public HealingResult(String elementKey, String originalLocator, String healedLocator,
                         double confidenceScore, String strategy, boolean successful) {
        this();
        this.elementKey = elementKey;
        this.originalLocator = originalLocator;
        this.healedLocator = healedLocator;
        this.confidenceScore = confidenceScore;
        this.strategy = strategy;
        this.successful = successful;
    }

    public String getElementKey() {
        return elementKey;
    }

    public void setElementKey(String elementKey) {
        this.elementKey = elementKey;
    }

    public String getOriginalLocator() {
        return originalLocator;
    }

    public void setOriginalLocator(String originalLocator) {
        this.originalLocator = originalLocator;
    }

    public String getHealedLocator() {
        return healedLocator;
    }

    public void setHealedLocator(String healedLocator) {
        this.healedLocator = healedLocator;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Override
    public String toString() {
        return "HealingResult{" +
                "elementKey='" + elementKey + '\'' +
                ", originalLocator='" + originalLocator + '\'' +
                ", healedLocator='" + healedLocator + '\'' +
                ", confidenceScore=" + String.format("%.2f%%", confidenceScore * 100) +
                ", strategy='" + strategy + '\'' +
                ", successful=" + successful +
                '}';
    }
}
