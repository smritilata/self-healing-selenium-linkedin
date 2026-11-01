package com.automation.healing;

import com.automation.healing.driver.DriverManager;
import com.automation.healing.driver.HealingDriver;
import com.automation.healing.model.HealingResult;
import com.automation.healing.model.JobDetails;
import com.automation.healing.pages.LinkedInJobsPage;
import com.automation.healing.report.HealingReportManager;
import com.automation.healing.util.ConfigLoader;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Standalone demonstration runner for Autonomous Self-Healing Selenium Job Search.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("  🛡️  AUTONOMOUS SELF-HEALING TEST AUTOMATION FRAMEWORK (JAVA + SELENIUM)  ");
        System.out.println("  Target Application: LinkedIn Public Job Search Automation                   ");
        System.out.println("===============================================================================\n");

        String keyword = ConfigLoader.get("linkedin.default.keyword", "Software QA Engineer");
        String location = ConfigLoader.get("linkedin.default.location", "Remote");
        int maxJobs = ConfigLoader.getInt("linkedin.max.jobs.to.extract", 5);

        logger.info("Starting Self-Healing Selenium demonstration run...");
        WebDriver rawDriver = DriverManager.createDriver();
        HealingDriver driver = new HealingDriver(rawDriver);

        try {
            LinkedInJobsPage jobsPage = new LinkedInJobsPage(driver);

            // Phase 1: Navigate to LinkedIn Jobs
            logger.info("Phase 1: Navigating to LinkedIn Job Search...");
            jobsPage.navigate();

            // Phase 2: Execute search using DELIBERATELY CORRUPTED locators to demonstrate healing
            System.out.println("\n-------------------------------------------------------------------------------");
            System.out.println("  PHASE 2: TESTING SELF-HEALING WITH BROKEN LOCATORS");
            System.out.println("  (Injecting invalid IDs and obsolete CSS to trigger healing engine)");
            System.out.println("-------------------------------------------------------------------------------\n");

            jobsPage.searchWithDeliberatelyBrokenLocators(keyword, location);

            // Phase 3: Extract and display job results
            System.out.println("\n-------------------------------------------------------------------------------");
            System.out.println("  PHASE 3: EXTRACTING LIVE JOB LISTINGS");
            System.out.println("-------------------------------------------------------------------------------\n");

            List<JobDetails> jobs = jobsPage.extractJobListings(maxJobs);

            System.out.println("\n===============================================================================");
            System.out.println(String.format("  EXTRACTED %d JOBS FOR '%s' (%s):", jobs.size(), keyword, location));
            System.out.println("===============================================================================");
            for (int i = 0; i < jobs.size(); i++) {
                JobDetails j = jobs.get(i);
                System.out.printf("[%d] %s%n    Company  : %s%n    Location : %s%n    Posted   : %s%n    Link     : %s%n%n",
                        i + 1, j.getTitle(), j.getCompany(), j.getLocation(), j.getDatePosted(), j.getJobUrl());
            }

            // Phase 4: Review Healing Audit Results
            System.out.println("===============================================================================");
            System.out.println("  SELF-HEALING AUDIT SUMMARY:");
            System.out.println("===============================================================================");
            List<HealingResult> healingResults = HealingReportManager.getInstance().getResults();
            for (HealingResult res : healingResults) {
                System.out.printf("- Element Key   : %s%n  Status        : %s%n  Strategy      : %s%n  Match Score   : %.1f%%%n  Broken Input  : %s%n  Healed To     : %s%n%n",
                        res.getElementKey(),
                        res.isSuccessful() ? "SUCCESS (HEALED)" : "FAILED",
                        res.getStrategy(),
                        res.getConfidenceScore() * 100,
                        res.getOriginalLocator(),
                        res.getHealedLocator());
            }

            // Phase 5: Generate HTML Dashboard Report
            String reportPath = HealingReportManager.getInstance().generateHtmlReport();
            System.out.println("===============================================================================");
            System.out.println("  REPORT GENERATED:");
            System.out.println("  " + reportPath);
            System.out.println("===============================================================================");

            if (reportPath != null) {
                try {
                    new ProcessBuilder("cmd", "/c", "start", "", reportPath).start();
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            logger.error("Error occurred during execution: ", e);
        } finally {
            logger.info("Cleaning up browser session...");
            driver.quit();
        }
    }
}
