package com.automation.healing.tests;

import com.automation.healing.driver.DriverManager;
import com.automation.healing.driver.HealingDriver;
import com.automation.healing.report.HealingReportManager;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

/**
 * Base Test class configuring the Healing WebDriver lifecycle and HTML report generation.
 */
public abstract class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected HealingDriver driver;

    @BeforeMethod
    public void setUp() {
        logger.info("Initializing browser session...");
        WebDriver rawDriver = DriverManager.createDriver();
        this.driver = new HealingDriver(rawDriver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            try {
                logger.info("Closing browser session...");
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error quitting driver: {}", e.getMessage());
            }
        }
    }

    @AfterSuite(alwaysRun = true)
    public void generateHealingReport() {
        logger.info("Generating Final Self-Healing Dashboard Report...");
        String reportPath = HealingReportManager.getInstance().generateHtmlReport();
        if (reportPath != null) {
            logger.info("Healing Report successfully created at: {}", reportPath);
        }
    }
}
