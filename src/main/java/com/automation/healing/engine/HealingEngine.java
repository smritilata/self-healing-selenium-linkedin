package com.automation.healing.engine;

import com.automation.healing.model.HealingResult;
import com.automation.healing.model.LocatorProfile;
import com.automation.healing.report.HealingReportManager;
import com.automation.healing.repository.LocatorRepository;
import com.automation.healing.util.ConfigLoader;
import com.automation.healing.util.SimilarityUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Intelligent self-healing core engine.
 * Employs a 3-tier recovery model:
 * 1. Fallback Locators Chain
 * 2. Multi-Attribute & Semantic DOM Similarity Scoring (Jaro-Winkler + Token Jaccard)
 * 3. Dynamic Selector Synthesis & Cache Auto-Update
 */
public class HealingEngine {

    private static final Logger logger = LoggerFactory.getLogger(HealingEngine.class);
    private static final double DEFAULT_THRESHOLD = 0.60;

    private final LocatorRepository repository;
    private final HealingReportManager reportManager;
    private final double similarityThreshold;
    private final boolean highlightEnabled;
    private final boolean screenshotEnabled;

    public HealingEngine() {
        this.repository = LocatorRepository.getInstance();
        this.reportManager = HealingReportManager.getInstance();
        this.similarityThreshold = ConfigLoader.getDouble("healing.similarity.threshold", DEFAULT_THRESHOLD);
        this.highlightEnabled = ConfigLoader.getBoolean("healing.highlight.healed", true);
        this.screenshotEnabled = ConfigLoader.getBoolean("healing.screenshot.on.heal", true);
    }

    /**
     * Attempts to heal a broken locator and return the working WebElement.
     */
    public WebElement heal(WebDriver driver, String elementKey, By brokenBy, Exception originalException) {
        logger.warn(">>> SELF-HEALING TRIGGERED for key '{}' (Original locator: {})", elementKey, brokenBy);

        LocatorProfile profile = repository.getProfile(elementKey);
        if (profile == null) {
            profile = synthesizeProfileFromBy(elementKey, brokenBy);
            repository.registerProfile(profile);
        }

        // Tier 1: Try stored fallback locators
        WebElement healedByFallback = tryFallbackLocators(driver, profile);
        if (healedByFallback != null) {
            return healedByFallback;
        }

        // Tier 2: Search DOM candidates via similarity matching
        WebElement healedBySimilarity = trySimilarityMatching(driver, profile, brokenBy);
        if (healedBySimilarity != null) {
            return healedBySimilarity;
        }

        // Tier 3: Search using semantic text or aria-label anchors
        WebElement healedByAnchor = tryAnchorSearch(driver, profile, brokenBy);
        if (healedByAnchor != null) {
            return healedByAnchor;
        }

        // If all tiers fail, record failure and throw descriptive exception
        reportManager.recordHealingResult(new HealingResult(
                elementKey,
                brokenBy.toString(),
                "NONE",
                0.0,
                "FAILED",
                false
        ));

        String msg = String.format("Self-Healing FAILED for element '%s' with original locator '%s'. " +
                        "No candidate element met the similarity threshold (>= %.2f).",
                elementKey, brokenBy, similarityThreshold);
        logger.error(msg);
        throw new org.openqa.selenium.NoSuchElementException(msg, originalException);
    }

    private WebElement tryFallbackLocators(WebDriver driver, LocatorProfile profile) {
        List<String> fallbacks = profile.getFallbackLocators();
        if (fallbacks == null || fallbacks.isEmpty()) {
            return null;
        }

        logger.info("Attempting Tier 1 Fallback evaluation ({} candidates) for '{}'...", fallbacks.size(), profile.getKey());

        for (String selector : fallbacks) {
            try {
                By by = selectorToBy(selector);
                List<WebElement> elements = driver.findElements(by);
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        logger.info(">>> SUCCESS: Healed '{}' using fallback selector: {}", profile.getKey(), selector);
                        completeHealingSuccess(driver, profile.getKey(), profile.getPrimaryLocatorValue(), selector, 0.95, "FALLBACK_SELECTOR", el);
                        return el;
                    }
                }
            } catch (Exception e) {
                logger.debug("Fallback '{}' did not match: {}", selector, e.getMessage());
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private WebElement trySimilarityMatching(WebDriver driver, LocatorProfile profile, By brokenBy) {
        if (!(driver instanceof JavascriptExecutor js)) {
            return null;
        }

        logger.info("Attempting Tier 2 Multi-Attribute Similarity Matching for '{}'...", profile.getKey());

        String targetTag = profile.getFingerprint().getOrDefault("tag", "input,button,a,div,span,select,time,h1,h2,h3,h4");

        // JS script collects candidate interactive elements and returns their attributes
        String script = """
            const tagFilter = arguments[0];
            const tags = tagFilter.split(',').map(s => s.trim().toLowerCase());
            let candidates = [];

            tags.forEach(tag => {
                const elems = document.getElementsByTagName(tag);
                for (let i = 0; i < elems.length && candidates.length < 150; i++) {
                    const el = elems[i];
                    const rect = el.getBoundingClientRect();
                    const isVisible = rect.width > 0 && rect.height > 0 && window.getComputedStyle(el).visibility !== 'hidden';
                    if (!isVisible) continue;

                    candidates.push({
                        tag: el.tagName.toLowerCase(),
                        id: el.id || '',
                        name: el.getAttribute('name') || '',
                        type: el.getAttribute('type') || '',
                        placeholder: el.getAttribute('placeholder') || '',
                        ariaLabel: el.getAttribute('aria-label') || '',
                        role: el.getAttribute('role') || '',
                        className: el.className ? String(el.className) : '',
                        text: (el.innerText || el.textContent || '').trim().substring(0, 100),
                        xpath: getElementXPath(el)
                    });
                }
            });

            function getElementXPath(element) {
                if (element.id !== '') return '//*[@id="' + element.id + '"]';
                if (element === document.body) return '/html/body';
                let ix = 0;
                const siblings = element.parentNode ? element.parentNode.childNodes : [];
                for (let i = 0; i < siblings.length; i++) {
                    const sibling = siblings[i];
                    if (sibling === element) {
                        return getElementXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';
                    }
                    if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
                        ix++;
                    }
                }
                return '';
            }

            return candidates;
        """;

        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) js.executeScript(script, targetTag);
            if (candidates == null || candidates.isEmpty()) {
                logger.warn("No DOM candidates extracted for tag filter: {}", targetTag);
                return null;
            }

            Map<String, String> expected = profile.getFingerprint();
            Map<String, Object> bestCandidate = null;
            double highestScore = 0.0;

            for (Map<String, Object> cand : candidates) {
                Map<String, String> actual = new HashMap<>();
                cand.forEach((k, v) -> actual.put(k.toLowerCase(Locale.ROOT), v == null ? "" : v.toString()));

                // Standardize aria-label key
                if (actual.containsKey("arialabel")) {
                    actual.put("aria-label", actual.get("arialabel"));
                }

                double score = SimilarityUtils.computeElementScore(expected, actual);
                if (score > highestScore) {
                    highestScore = score;
                    bestCandidate = cand;
                }
            }

            logger.info("Similarity candidate analysis for '{}': Best match score = {} (Threshold: {})",
                    profile.getKey(), String.format("%.2f%%", highestScore * 100), String.format("%.2f%%", similarityThreshold * 100));

            if (highestScore >= similarityThreshold && bestCandidate != null) {
                String candidateXpath = (String) bestCandidate.get("xpath");
                String generatedSelector = synthesizeRobustSelector(bestCandidate);

                // Attempt locating via robust selector first, then xpath
                WebElement healedElement = null;
                String finalUsedSelector = generatedSelector;

                try {
                    By robustBy = selectorToBy(generatedSelector);
                    WebElement el = driver.findElement(robustBy);
                    if (el.isDisplayed()) {
                        healedElement = el;
                    }
                } catch (Exception ignored) {
                    // Fall back to unique DOM XPath
                    try {
                        By xpathBy = By.xpath(candidateXpath);
                        WebElement el = driver.findElement(xpathBy);
                        if (el.isDisplayed()) {
                            healedElement = el;
                            finalUsedSelector = candidateXpath;
                        }
                    } catch (Exception ex2) {
                        logger.debug("Failed locating candidate via XPath: {}", ex2.getMessage());
                    }
                }

                if (healedElement != null) {
                    logger.info(">>> SUCCESS: Healed '{}' via similarity matching ({}) with new selector: {}",
                            profile.getKey(), String.format("%.2f%%", highestScore * 100), finalUsedSelector);
                    completeHealingSuccess(driver, profile.getKey(), brokenBy.toString(), finalUsedSelector, highestScore, "SIMILARITY_MATCHING", healedElement);
                    return healedElement;
                }
            }
        } catch (Exception e) {
            logger.warn("Similarity matching evaluation encountered an issue: {}", e.getMessage());
        }

        return null;
    }

    private WebElement tryAnchorSearch(WebDriver driver, LocatorProfile profile, By brokenBy) {
        String aria = profile.getFingerprint().get("aria-label");
        String placeholder = profile.getFingerprint().get("placeholder");
        String text = profile.getFingerprint().get("text");

        List<String> anchorXPaths = new ArrayList<>();
        if (aria != null && !aria.isBlank()) {
            anchorXPaths.add(String.format("//*[@aria-label='%s' or contains(@aria-label, '%s')]", aria, aria));
        }
        if (placeholder != null && !placeholder.isBlank()) {
            anchorXPaths.add(String.format("//*[@placeholder='%s' or contains(@placeholder, '%s')]", placeholder, placeholder));
        }
        if (text != null && !text.isBlank() && text.length() > 2) {
            anchorXPaths.add(String.format("//*[contains(normalize-space(text()), '%s')]", text));
        }

        for (String xpath : anchorXPaths) {
            try {
                WebElement el = driver.findElement(By.xpath(xpath));
                if (el.isDisplayed()) {
                    logger.info(">>> SUCCESS: Healed '{}' via semantic anchor: {}", profile.getKey(), xpath);
                    completeHealingSuccess(driver, profile.getKey(), brokenBy.toString(), xpath, 0.85, "STRUCTURAL_ANCHOR", el);
                    return el;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String synthesizeRobustSelector(Map<String, Object> candidate) {
        String tag = String.valueOf(candidate.getOrDefault("tag", "*"));
        String name = String.valueOf(candidate.getOrDefault("name", ""));
        String aria = String.valueOf(candidate.getOrDefault("ariaLabel", ""));
        String placeholder = String.valueOf(candidate.getOrDefault("placeholder", ""));
        String id = String.valueOf(candidate.getOrDefault("id", ""));

        if (!name.isBlank()) {
            return String.format("%s[name='%s']", tag, name);
        }
        if (!aria.isBlank()) {
            return String.format("%s[aria-label='%s']", tag, aria);
        }
        if (!placeholder.isBlank()) {
            return String.format("%s[placeholder='%s']", tag, placeholder);
        }
        if (!id.isBlank() && !id.matches(".*\\d{4,}.*")) { // avoid dynamic random ids
            return String.format("#%s", id);
        }
        return String.valueOf(candidate.get("xpath"));
    }

    private void completeHealingSuccess(WebDriver driver, String key, String origLocator, String healedLocator,
                                        double score, String strategy, WebElement element) {
        // Visual decoration
        if (highlightEnabled) {
            VisualHighlighter.highlightHealedElement(driver, element, key, score);
        }

        // Screenshot capture
        String screenshotPath = null;
        if (screenshotEnabled) {
            screenshotPath = VisualHighlighter.captureHealedScreenshot(driver, key);
        }

        // Update repository cache
        repository.recordHealingSuccess(key, healedLocator, score);

        // Record report entry
        HealingResult result = new HealingResult(key, origLocator, healedLocator, score, strategy, true);
        result.setScreenshotPath(screenshotPath);
        reportManager.recordHealingResult(result);
    }

    public static By selectorToBy(String selector) {
        if (selector.startsWith("//") || selector.startsWith("(") || selector.startsWith("/html")) {
            return By.xpath(selector);
        }
        return By.cssSelector(selector);
    }

    private LocatorProfile synthesizeProfileFromBy(String key, By by) {
        String str = by.toString();
        LocatorProfile profile = new LocatorProfile(key, "unknown", str);
        if (str.startsWith("By.xpath: ")) {
            profile.setPrimaryLocatorType("xpath");
            profile.setPrimaryLocatorValue(str.substring(10));
        } else if (str.startsWith("By.cssSelector: ")) {
            profile.setPrimaryLocatorType("css");
            profile.setPrimaryLocatorValue(str.substring(16));
        } else if (str.startsWith("By.id: ")) {
            profile.setPrimaryLocatorType("id");
            profile.setPrimaryLocatorValue(str.substring(7));
            profile.addAttribute("id", str.substring(7));
        } else if (str.startsWith("By.name: ")) {
            profile.setPrimaryLocatorType("name");
            profile.setPrimaryLocatorValue(str.substring(9));
            profile.addAttribute("name", str.substring(9));
        }
        return profile;
    }
}
