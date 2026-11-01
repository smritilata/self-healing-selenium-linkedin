package com.automation.healing.tests;

import com.automation.healing.model.HealingResult;
import com.automation.healing.model.JobDetails;
import com.automation.healing.pages.LinkedInJobsPage;
import com.automation.healing.report.HealingReportManager;
import com.automation.healing.util.ConfigLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * End-to-End Test Suite for LinkedIn Job Search Automation with Autonomous Self-Healing Locators.
 */
public class LinkedInJobSearchTest extends BaseTest {

    @Test(priority = 1, description = "Verifies standard LinkedIn Job Search and listing extraction")
    public void testStandardJobSearch() {
        String keyword = ConfigLoader.get("linkedin.default.keyword", "SDET");
        String location = ConfigLoader.get("linkedin.default.location", "Remote");
        int maxJobs = ConfigLoader.getInt("linkedin.max.jobs.to.extract", 6);

        logger.info(">>> TEST 1: Executing standard LinkedIn job search for '{}' in '{}'", keyword, location);

        LinkedInJobsPage jobsPage = new LinkedInJobsPage(driver);
        jobsPage.navigate()
                .searchJobs(keyword, location);

        List<JobDetails> jobs = jobsPage.extractJobListings(maxJobs);

        logger.info("Extracted {} jobs from search results.", jobs.size());
        for (int i = 0; i < jobs.size(); i++) {
            logger.info("[{}] {}", i + 1, jobs.get(i));
        }

        Assert.assertFalse(jobs.isEmpty(), "Expected at least one job to be extracted from LinkedIn public search.");
    }

    @Test(priority = 2, description = "Verifies Autonomous Self-Healing when locators are deliberately corrupted/broken")
    public void testSelfHealingJobSearchWithBrokenLocators() {
        String keyword = "Java Automation Engineer";
        String location = "Remote";
        int maxJobs = 5;

        logger.info(">>> TEST 2: Executing job search with DELIBERATELY CORRUPTED locators to test Self-Healing...");

        int healEventsBefore = HealingReportManager.getInstance().getResults().size();

        LinkedInJobsPage jobsPage = new LinkedInJobsPage(driver);
        jobsPage.navigate()
                .searchWithDeliberatelyBrokenLocators(keyword, location);

        List<JobDetails> jobs = jobsPage.extractJobListings(maxJobs);

        int healEventsAfter = HealingReportManager.getInstance().getResults().size();
        int healedCount = healEventsAfter - healEventsBefore;

        logger.info("Self-Healing Test Completed! Healed {} broken locators automatically.", healedCount);

        List<HealingResult> latestResults = HealingReportManager.getInstance().getResults();
        for (HealingResult res : latestResults) {
            logger.info("Healing Audit -> Key: '{}' | Status: {} | Strategy: {} | Score: {} | Broken: {} -> Healed: {}",
                    res.getElementKey(),
                    res.isSuccessful() ? "SUCCESS" : "FAILED",
                    res.getStrategy(),
                    String.format("%.1f%%", res.getConfidenceScore() * 100),
                    res.getOriginalLocator(),
                    res.getHealedLocator());
        }

        // Verify that self-healing actually triggered and succeeded
        Assert.assertTrue(healedCount > 0, "Self-healing should have intervened and healed at least one broken locator.");
        Assert.assertFalse(jobs.isEmpty(), "Search should have succeeded and returned job listings despite broken locators.");
    }
}
