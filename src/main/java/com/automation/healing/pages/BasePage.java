package com.automation.healing.pages;

import com.automation.healing.driver.HealingDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Page containing shared page utilities and healing driver integration.
 */
public abstract class BasePage {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final HealingDriver driver;

    public BasePage(HealingDriver driver) {
        this.driver = driver;
    }

    protected void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected WebElement find(String elementKey, By primaryBy) {
        return driver.findElement(elementKey, primaryBy);
    }

    protected void click(String elementKey, By primaryBy) {
        logger.info("Clicking element '{}' (Primary: {})", elementKey, primaryBy);
        driver.safeClick(elementKey, primaryBy);
    }

    protected void type(String elementKey, By primaryBy, String text) {
        logger.info("Typing '{}' into element '{}' (Primary: {})", text, elementKey, primaryBy);
        driver.safeType(elementKey, primaryBy, text);
    }
}
