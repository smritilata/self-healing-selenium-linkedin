package com.automation.healing.pages;

import com.automation.healing.driver.HealingDriver;
import com.automation.healing.model.JobDetails;
import com.automation.healing.util.ConfigLoader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for LinkedIn Job Search page (https://www.linkedin.com/jobs/search/).
 * Equipped with self-healing locator bindings.
 */
public class LinkedInJobsPage extends BasePage {

    private final String baseUrl;

    // Standard primary locators (which match locators-repo.json keys)
    public static final String KEY_KEYWORDS = "linkedin.keywords_input";
    public static final String KEY_LOCATION = "linkedin.location_input";
    public static final String KEY_SEARCH_BTN = "linkedin.search_button";
    public static final String KEY_JOB_LIST = "linkedin.job_cards_list";
    public static final String KEY_JOB_CARD = "linkedin.job_card";
    public static final String KEY_DISMISS_MODAL = "linkedin.dismiss_modal_button";

    private final By defaultKeywordsBy = By.cssSelector("input[name='keywords']");
    private final By defaultLocationBy = By.cssSelector("input[name='location']");
    private final By defaultSearchBtnBy = By.cssSelector("button[data-tracking-control-name='public_jobs_jobs-search-box_search-submit']");
    private final By defaultJobCardsListBy = By.cssSelector("ul.jobs-search__results-list");
    private final By defaultDismissModalBy = By.cssSelector("button[data-tracking-control-name='public_jobs_contextual-sign-in-modal_modal_dismiss']");

    public LinkedInJobsPage(HealingDriver driver) {
        super(driver);
        this.baseUrl = ConfigLoader.get("linkedin.url", "https://www.linkedin.com/jobs/search/");
    }

    /**
     * Navigates to the public LinkedIn Job search page.
     */
    public LinkedInJobsPage navigate() {
        logger.info("Navigating to LinkedIn Jobs: {}", baseUrl);
        driver.get(baseUrl);
        pause(2000);
        dismissSignInModalIfPresent();
        return this;
    }

    /**
     * Dismisses sign-in dialog or auth modal if LinkedIn prompts guest users.
     */
    public void dismissSignInModalIfPresent() {
        try {
            List<WebElement> dismissBtns = driver.findElements(defaultDismissModalBy);
            if (!dismissBtns.isEmpty() && dismissBtns.get(0).isDisplayed()) {
                logger.info("Guest modal detected, dismissing...");
                dismissBtns.get(0).click();
                pause(500);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Performs standard job search with primary locators.
     */
    public LinkedInJobsPage searchJobs(String keyword, String location) {
        logger.info("Executing LinkedIn Job Search -> Keywords: '{}', Location: '{}'", keyword, location);

        dismissSignInModalIfPresent();

        // 1. Enter Keywords
        WebElement keywordField = find(KEY_KEYWORDS, defaultKeywordsBy);
        keywordField.clear();
        keywordField.sendKeys(keyword);
        pause(400);

        // 2. Enter Location
        WebElement locationField = find(KEY_LOCATION, defaultLocationBy);
        locationField.clear();
        locationField.sendKeys(location);
        pause(400);

        // 3. Click Search Button
        try {
            click(KEY_SEARCH_BTN, defaultSearchBtnBy);
        } catch (Exception e) {
            logger.info("Direct button click failed, pressing ENTER on location field...");
            locationField.sendKeys(Keys.ENTER);
        }

        pause(3000);
        dismissSignInModalIfPresent();
        return this;
    }

    /**
     * Demonstrates SELF-HEALING by intentionally supplying broken / obsolete locators.
     * The framework will intercept the failures, trigger the healing engine, score candidate elements,
     * highlight them, update the repository, and proceed with the search without breaking!
     */
    public LinkedInJobsPage searchWithDeliberatelyBrokenLocators(String keyword, String location) {
        logger.warn("=======================================================================");
        logger.warn("DEMONSTRATION: Invoking job search with DELIBERATELY BROKEN LOCATORS...");
        logger.warn("=======================================================================");

        dismissSignInModalIfPresent();

        // Intentional broken locators that do NOT exist in the DOM:
        By brokenKeywordsBy = By.id("obsolete-broken-keyword-input-9999");
        By brokenLocationBy = By.cssSelector("input.nonexistent-legacy-location-picker");
        By brokenSearchBtnBy = By.xpath("//button[@id='corrupted-search-submit-btn-404']");

        // The healing driver intercepts and heals all 3!
        logger.info("Attempting to type keyword using BROKEN selector: {}", brokenKeywordsBy);
        WebElement keywordField = find(KEY_KEYWORDS, brokenKeywordsBy);
        keywordField.clear();
        keywordField.sendKeys(keyword);
        pause(600);

        logger.info("Attempting to type location using BROKEN selector: {}", brokenLocationBy);
        WebElement locationField = find(KEY_LOCATION, brokenLocationBy);
        locationField.clear();
        locationField.sendKeys(location);
        pause(600);

        logger.info("Attempting to click search using BROKEN selector: {}", brokenSearchBtnBy);
        try {
            click(KEY_SEARCH_BTN, brokenSearchBtnBy);
        } catch (Exception e) {
            logger.info("Submitting via ENTER key fallback on healed location field...");
            locationField.sendKeys(Keys.ENTER);
        }

        pause(3000);
        dismissSignInModalIfPresent();
        return this;
    }

    /**
     * Extracts job listings from search results.
     */
    public List<JobDetails> extractJobListings(int maxCount) {
        logger.info("Extracting up to {} job listings from search results...", maxCount);
        List<JobDetails> jobs = new ArrayList<>();

        // Scroll slightly to trigger dynamic lazy loading of job cards
        try {
            ((JavascriptExecutor) driver.getDelegate()).executeScript("window.scrollBy(0, 600);");
            pause(1500);
        } catch (Exception ignored) {}

        // Locate job card elements
        By cardBy = By.cssSelector("ul.jobs-search__results-list > li, div.base-search-card, li.jobs-search-results__list-item");
        List<WebElement> cards = driver.findElements(KEY_JOB_CARD, cardBy);

        logger.info("Found {} raw job cards on the page.", cards.size());

        int count = 0;
        for (WebElement card : cards) {
            if (count >= maxCount) break;

            try {
                String title = extractChildText(card, By.cssSelector("h3.base-search-card__title, .job-search-card__title, a.base-card__full-link"));
                String company = extractChildText(card, By.cssSelector("h4.base-search-card__subtitle, .job-search-card__subtitle, a.hidden-nested-link"));
                String location = extractChildText(card, By.cssSelector(".job-search-card__location, .base-search-card__metadata-item"));
                String datePosted = extractChildText(card, By.cssSelector("time"));
                String jobUrl = extractAttribute(card, By.cssSelector("a.base-card__full-link"), "href");

                if (title != null && !title.isBlank()) {
                    JobDetails job = new JobDetails(title, company, location, datePosted, jobUrl);
                    jobs.add(job);
                    count++;
                    logger.info("Extracted Job [{}]: '{}' at '{}' ({})", count, title, company, location);
                }
            } catch (Exception e) {
                logger.debug("Skipping unparseable card: {}", e.getMessage());
            }
        }

        return jobs;
    }

    private String extractChildText(WebElement parent, By childBy) {
        try {
            List<WebElement> elements = parent.findElements(childBy);
            if (!elements.isEmpty()) {
                return elements.get(0).getText().trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractAttribute(WebElement parent, By childBy, String attribute) {
        try {
            List<WebElement> elements = parent.findElements(childBy);
            if (!elements.isEmpty()) {
                return elements.get(0).getAttribute(attribute);
            }
        } catch (Exception ignored) {}
        return "";
    }
}
