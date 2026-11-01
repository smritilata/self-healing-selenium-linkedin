package com.automation.healing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.*;

/**
 * Represents the multi-dimensional fingerprint and locator strategies for a web element.
 * Enables the Self-Healing engine to reconstruct and identify broken selectors at runtime.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocatorProfile implements Serializable {

    private String key;
    private String primaryLocatorType; // "xpath", "css", "id", "name"
    private String primaryLocatorValue;
    private List<String> fallbackLocators = new ArrayList<>();
    private Map<String, String> fingerprint = new HashMap<>();
    private int healCount = 0;
    private String lastHealedTimestamp;
    private String lastKnownWorkingLocator;

    public LocatorProfile() {
    }

    public LocatorProfile(String key, String primaryLocatorType, String primaryLocatorValue) {
        this.key = key;
        this.primaryLocatorType = primaryLocatorType;
        this.primaryLocatorValue = primaryLocatorValue;
        this.lastKnownWorkingLocator = primaryLocatorValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPrimaryLocatorType() {
        return primaryLocatorType;
    }

    public void setPrimaryLocatorType(String primaryLocatorType) {
        this.primaryLocatorType = primaryLocatorType;
    }

    public String getPrimaryLocatorValue() {
        return primaryLocatorValue;
    }

    public void setPrimaryLocatorValue(String primaryLocatorValue) {
        this.primaryLocatorValue = primaryLocatorValue;
    }

    public List<String> getFallbackLocators() {
        return fallbackLocators;
    }

    public void setFallbackLocators(List<String> fallbackLocators) {
        this.fallbackLocators = fallbackLocators != null ? fallbackLocators : new ArrayList<>();
    }

    public void addFallbackLocator(String fallback) {
        if (fallback != null && !fallback.isBlank() && !this.fallbackLocators.contains(fallback)) {
            this.fallbackLocators.add(fallback);
        }
    }

    public Map<String, String> getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(Map<String, String> fingerprint) {
        this.fingerprint = fingerprint != null ? fingerprint : new HashMap<>();
    }

    public void addAttribute(String name, String value) {
        if (name != null && value != null) {
            this.fingerprint.put(name.toLowerCase(Locale.ROOT), value);
        }
    }

    public int getHealCount() {
        return healCount;
    }

    public void setHealCount(int healCount) {
        this.healCount = healCount;
    }

    public void incrementHealCount() {
        this.healCount++;
    }

    public String getLastHealedTimestamp() {
        return lastHealedTimestamp;
    }

    public void setLastHealedTimestamp(String lastHealedTimestamp) {
        this.lastHealedTimestamp = lastHealedTimestamp;
    }

    public String getLastKnownWorkingLocator() {
        return lastKnownWorkingLocator;
    }

    public void setLastKnownWorkingLocator(String lastKnownWorkingLocator) {
        this.lastKnownWorkingLocator = lastKnownWorkingLocator;
    }

    @Override
    public String toString() {
        return "LocatorProfile{" +
                "key='" + key + '\'' +
                ", primaryType='" + primaryLocatorType + '\'' +
                ", primaryValue='" + primaryLocatorValue + '\'' +
                ", fallbackCount=" + fallbackLocators.size() +
                ", healCount=" + healCount +
                '}';
    }
}
