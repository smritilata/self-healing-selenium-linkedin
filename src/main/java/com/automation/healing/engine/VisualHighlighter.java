package com.automation.healing.engine;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Visual feedback utility that decorates self-healed elements with animation and badges in the live browser,
 * and captures timestamped proof screenshots for the healing report.
 */
public class VisualHighlighter {

    private static final Logger logger = LoggerFactory.getLogger(VisualHighlighter.class);

    private VisualHighlighter() {}

    /**
     * Highlights an element with an animated glowing border and a floating tooltip badge.
     */
    public static void highlightHealedElement(WebDriver driver, WebElement element, String elementKey, double score) {
        if (!(driver instanceof JavascriptExecutor js)) {
            return;
        }

        try {
            int scorePercent = (int) Math.round(score * 100);
            String badgeText = String.format("HEALED: %s (%d%% match)", elementKey, scorePercent);

            String script = """
                const el = arguments[0];
                const badgeText = arguments[1];

                // Add bright border
                const origOutline = el.style.outline;
                const origBoxShadow = el.style.boxShadow;
                const origTransition = el.style.transition;

                el.style.transition = 'all 0.3s ease-in-out';
                el.style.outline = '3px dashed #10b981';
                el.style.boxShadow = '0 0 15px rgba(16, 185, 129, 0.7)';

                // Create floating badge
                const badge = document.createElement('div');
                badge.className = 'self-healing-badge';
                badge.innerText = badgeText;
                badge.style.position = 'absolute';
                badge.style.zIndex = '999999';
                badge.style.background = 'linear-gradient(135deg, #059669, #10b981)';
                badge.style.color = '#ffffff';
                badge.style.padding = '3px 8px';
                badge.style.fontSize = '12px';
                badge.style.fontWeight = 'bold';
                badge.style.borderRadius = '4px';
                badge.style.boxShadow = '0 2px 8px rgba(0,0,0,0.3)';
                badge.style.pointerEvents = 'none';

                const rect = el.getBoundingClientRect();
                badge.style.top = (window.scrollY + rect.top - 24) + 'px';
                badge.style.left = (window.scrollX + rect.left) + 'px';

                document.body.appendChild(badge);

                // Revert after 3.5 seconds
                setTimeout(() => {
                    el.style.outline = origOutline;
                    el.style.boxShadow = origBoxShadow;
                    el.style.transition = origTransition;
                    if (badge.parentNode) {
                        badge.parentNode.removeChild(badge);
                    }
                }, 3500);
            """;

            js.executeScript(script, element, badgeText);
        } catch (Exception e) {
            logger.debug("Could not inject visual highlight on element: {}", e.getMessage());
        }
    }

    /**
     * Takes screenshot and stores it in the reports directory.
     */
    public static String captureHealedScreenshot(WebDriver driver, String elementKey) {
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            return null;
        }

        try {
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            String dirPath = "target/healing-reports/screenshots";
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = String.format("healed_%s_%s.png", elementKey.replaceAll("[^a-zA-Z0-9_-]", "_"), timestamp);
            File dest = dir.resolve(filename).toFile();

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }

            logger.info("Saved healing screenshot: {}", dest.getAbsolutePath());
            return dest.getAbsolutePath();
        } catch (Exception e) {
            logger.warn("Failed to capture screenshot for healing event: {}", e.getMessage());
            return null;
        }
    }
}
