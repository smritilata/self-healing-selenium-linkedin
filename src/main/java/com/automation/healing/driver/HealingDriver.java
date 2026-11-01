package com.automation.healing.driver;

import com.automation.healing.engine.HealingEngine;
import com.automation.healing.repository.LocatorRepository;
import com.automation.healing.util.ConfigLoader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Intelligent WebDriver decorator that seamlessly intercepts element lookup failures
 * and applies autonomous self-healing algorithms.
 */
public class HealingDriver implements WebDriver, JavascriptExecutor, TakesScreenshot {

    private static final Logger logger = LoggerFactory.getLogger(HealingDriver.class);

    private final WebDriver delegate;
    private final HealingEngine healingEngine;
    private final boolean healingEnabled;
    private final int explicitWaitSeconds;

    public HealingDriver(WebDriver delegate) {
        this.delegate = delegate;
        this.healingEngine = new HealingEngine();
        this.healingEnabled = ConfigLoader.getBoolean("healing.enabled", true);
        this.explicitWaitSeconds = ConfigLoader.getInt("explicitWaitSeconds", 10);
    }

    public WebDriver getDelegate() {
        return delegate;
    }

    /**
     * Core self-healing locator lookup method with explicit key mapping.
     */
    public WebElement findElement(String elementKey, By primaryBy) {
        try {
            WebDriverWait wait = new WebDriverWait(delegate, Duration.ofSeconds(explicitWaitSeconds));
            return wait.until(d -> {
                WebElement el = d.findElement(primaryBy);
                return el.isDisplayed() ? el : null;
            });
        } catch (Exception ex) {
            if (!healingEnabled) {
                logger.error("Element '{}' not found and self-healing is disabled.", elementKey);
                throw ex;
            }
            logger.warn("Primary locator {} for key '{}' failed. Invoking Self-Healing Engine...", primaryBy, elementKey);
            return healingEngine.heal(delegate, elementKey, primaryBy, ex);
        }
    }

    /**
     * Standard findElement override with automatic key derivation and fallback healing.
     */
    @Override
    public WebElement findElement(By by) {
        try {
            return delegate.findElement(by);
        } catch (Exception ex) {
            if (!healingEnabled) {
                throw ex;
            }
            // Reverse lookup key from known repository or synthesize dynamic key
            String derivedKey = reverseLookupKey(by);
            logger.warn("Standard lookup for {} failed. Attempting autonomous healing with derived key '{}'...", by, derivedKey);
            return healingEngine.heal(delegate, derivedKey, by, ex);
        }
    }

    /**
     * Finds elements with key reference. Falls back to healing if initial list is empty.
     */
    public List<WebElement> findElements(String elementKey, By primaryBy) {
        List<WebElement> elements = delegate.findElements(primaryBy);
        if (elements.isEmpty() && healingEnabled) {
            logger.warn("Elements list for '{}' ({}) was empty. Attempting healing...", elementKey, primaryBy);
            try {
                WebElement healed = healingEngine.heal(delegate, elementKey, primaryBy, new NoSuchElementException("Empty elements list"));
                return List.of(healed);
            } catch (Exception e) {
                logger.debug("Could not heal collection for '{}': {}", elementKey, e.getMessage());
            }
        }
        return elements;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return delegate.findElements(by);
    }

    private String reverseLookupKey(By by) {
        String selectorStr = by.toString();
        var allProfiles = LocatorRepository.getInstance().getAllProfiles();
        for (var entry : allProfiles.entrySet()) {
            if (selectorStr.contains(entry.getValue().getPrimaryLocatorValue())) {
                return entry.getKey();
            }
            for (String fallback : entry.getValue().getFallbackLocators()) {
                if (selectorStr.contains(fallback)) {
                    return entry.getKey();
                }
            }
        }
        return "dynamic." + selectorStr.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    public void scrollToElement(WebElement element) {
        try {
            ((JavascriptExecutor) delegate).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(400);
        } catch (Exception ignored) {}
    }

    public void safeClick(String elementKey, By primaryBy) {
        WebElement element = findElement(elementKey, primaryBy);
        scrollToElement(element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            logger.info("Click intercepted on '{}', using JavaScript click fallback.", elementKey);
            ((JavascriptExecutor) delegate).executeScript("arguments[0].click();", element);
        }
    }

    public void safeType(String elementKey, By primaryBy, String text) {
        WebElement element = findElement(elementKey, primaryBy);
        scrollToElement(element);
        element.clear();
        element.sendKeys(text);
    }

    // --- Delegate Methods for WebDriver ---

    @Override
    public void get(String url) {
        delegate.get(url);
    }

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void quit() {
        delegate.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }

    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }

    @Override
    public Options manage() {
        return delegate.manage();
    }

    // --- Delegate Methods for JavascriptExecutor ---

    @Override
    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) delegate).executeScript(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return ((JavascriptExecutor) delegate).executeAsyncScript(script, args);
    }

    // --- Delegate Methods for TakesScreenshot ---

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return ((TakesScreenshot) delegate).getScreenshotAs(target);
    }
}
