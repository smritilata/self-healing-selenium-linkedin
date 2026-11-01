# 🛡️ Autonomous Self-Healing Selenium Automation (Java)

An enterprise-grade, autonomous **Self-Healing Test Automation Framework** written in **Java & Selenium WebDriver 4**, featuring an end-to-end implementation for **LinkedIn Job Search Automation**.

When web applications (like LinkedIn) update their UI, modify attribute names, or generate random dynamic CSS classes (e.g., `ember-view`, hashed classes), traditional Selenium tests crash with `NoSuchElementException`. This framework detects broken locators in real-time, inspects the live DOM, scores candidate elements using multi-attribute similarity algorithms, visually heals them in the browser, auto-updates the locator repository, and continues execution without human intervention.

---

## 🚀 Key Highlights & Architecture

```
                          ┌────────────────────────────┐
                          │   Test / Page Object Call  │
                          └─────────────┬──────────────┘
                                        ▼
                     ┌──────────────────────────────────────┐
                     │ HealingDriver.findElement(key, by)   │
                     └──────────────────┬───────────────────┘
                                        │
                         [ Try Primary Locator ]
                                        ├────────────── (Success) ──► Return WebElement
                                        │
                               (NoSuchElementException)
                                        ▼
                     ┌──────────────────────────────────────┐
                     │         HEALING ENGINE CORE          │
                     └──────────────────┬───────────────────┘
                                        │
           ┌────────────────────────────┼───────────────────────────┐
           ▼                            ▼                           ▼
 ┌───────────────────┐       ┌───────────────────────┐    ┌────────────────────┐
 │  Tier 1: Fallback │       │  Tier 2: DOM Scanner  │    │  Tier 3: Semantic  │
 │  Locators Chain   │       │  & Similarity Scoring │    │  & Aria Anchors    │
 └─────────┬─────────┘       └──────────┬────────────┘    └─────────┬──────────┘
           │                            │                           │
           └────────────────────────────┼───────────────────────────┘
                                        │ (Candidate match >= threshold)
                                        ▼
                     ┌──────────────────────────────────────┐
                     │  Synthesize & Verify New Selector    │
                     └──────────────────┬───────────────────┘
                                        │
           ┌────────────────────────────┴───────────────────────────┐
           ▼                                                        ▼
┌───────────────────────────────┐              ┌───────────────────────────────┐
│ Live In-Browser Highlight &   │              │  Auto-Update Repository &     │
│ Proof Screenshot Captured     │              │  Generate HTML Dashboard      │
└───────────────────────────────┘              └───────────────────────────────┘
```

1. **Multi-Strategy 3-Tier Healing**:
   - **Tier 1 (Fallback Locators)**: Evaluates cached alternative CSS/XPath selectors.
   - **Tier 2 (Multi-Attribute Similarity Scoring)**: Executes JavaScript DOM analysis to extract visible candidate elements and computes weighted Jaro-Winkler distance and Jaccard token similarity across `tag`, `name`, `type`, `placeholder`, `aria-label`, `role`, `text`, and `class`.
   - **Tier 3 (Semantic & Accessibility Anchors)**: Searches for resilient parent, sibling, or accessibility aria anchors.
2. **Auto-Learning & Persistent Repository (`locators-repo.json`)**:
   - Stores element fingerprints and historical working selectors.
   - Automatically saves healed selectors back to the repository so subsequent runs execute instantly without needing to heal again.
3. **Live Visual Highlight & Proof Screenshot**:
   - Injects a pulsating green outline and floating `[HEALED: 95% match]` badge on the live page before interacting.
   - Saves timestamped screenshots of healed elements to `target/healing-reports/screenshots/`.
4. **Interactive HTML Dashboard (`healing-report.html`)**:
   - Summarizes total healing events, success rates, broken vs healed selector diffs, confidence scores, and screenshot evidence.
5. **Real-World LinkedIn Job Search Automation**:
   - Targets public LinkedIn Job Search (`https://www.linkedin.com/jobs/search/`) — no login credentials or CAPTCHAs required.
   - Automatically dismisses guest sign-in modals.
   - Inputs job keyword (e.g. `SDET`) and location (e.g. `Remote`).
   - Extracts structured job listings (`JobDetails`: Title, Company, Location, Date Posted, URL).

---

## 📁 Project Structure

```
self-healing-selenium-linkedin/
├── pom.xml                                  # Maven dependencies (Selenium 4, TestNG, Jackson, Commons-Text)
├── testng.xml                               # TestNG execution suite
├── README.md                                # Full documentation
└── src
    ├── main
    │   ├── java/com/automation/healing
    │   │   ├── Main.java                    # Standalone runnable demo application
    │   │   ├── driver
    │   │   │   ├── DriverManager.java       # Browser initialization (Chrome, Firefox, Edge)
    │   │   │   └── HealingDriver.java       # Smart WebDriver decorator with autonomous healing
    │   │   ├── engine
    │   │   │   ├── HealingEngine.java       # 3-tier healing decision core
    │   │   │   └── VisualHighlighter.java   # Browser CSS badge injection & screenshot capture
    │   │   ├── model
    │   │   │   ├── HealingResult.java       # Audit record for healed events
    │   │   │   ├── JobDetails.java          # Extracted LinkedIn job data model
    │   │   │   └── LocatorProfile.java      # Element fingerprint & selector repository model
    │   │   ├── pages
    │   │   │   ├── BasePage.java            # Page Object Model base class
    │   │   │   └── LinkedInJobsPage.java    # LinkedIn job search actions & broken selector demos
    │   │   ├── report
    │   │   │   └── HealingReportManager.java# Generates responsive HTML healing dashboard
    │   │   ├── repository
    │   │   │   └── LocatorRepository.java   # JSON persistence manager for locators
    │   │   └── util
    │   │       ├── ConfigLoader.java        # Configuration properties reader
    │   │       └── SimilarityUtils.java     # Jaro-Winkler & Jaccard token similarity engine
    │   └── resources
    │       ├── config.properties            # Engine thresholds, timeouts, and browser flags
    │       ├── locators-repo.json           # Element fingerprints for LinkedIn Job Search
    │       └── logback.xml                  # Logging configuration
    └── test
        └── java/com/automation/healing/tests
            ├── BaseTest.java                # TestNG setup, teardown, and report generation
            └── LinkedInJobSearchTest.java   # Standard search test & broken locator healing test
```

---

## ⚙️ Configuration (`src/main/resources/config.properties`)

| Property | Default | Description |
|---|---|---|
| `browser` | `chrome` | Target browser (`chrome`, `firefox`, `edge`) |
| `headless` | `false` | Run browser in headless mode (`true`/`false`) |
| `healing.enabled` | `true` | Toggle self-healing on or off |
| `healing.similarity.threshold` | `0.60` | Minimum confidence score (0.0 to 1.0) to accept a candidate |
| `healing.highlight.healed` | `true` | Injects glowing badge on healed elements |
| `healing.auto.update.repo` | `true` | Persists repaired selectors to `locators-repo.json` |
| `healing.screenshot.on.heal` | `true` | Takes screenshot whenever an element is healed |
| `linkedin.default.keyword` | `SDET` | Default search keyword |
| `linkedin.default.location` | `Remote` | Default search location |
| `linkedin.max.jobs.to.extract`| `8` | Number of job listings to extract |

---

## 🧪 How to Run

### Requirements
- **Java 17+** (JDK 17 or higher)
- **Maven 3.8+**
- Google Chrome (or Edge/Firefox) installed

### Option 1: Run the Standalone Demonstration
Run the standalone runner directly via Maven or in your IDE:

```bash
mvn clean compile exec:java
```

This will:
1. Open Chrome and navigate to LinkedIn Jobs.
2. Intentionally execute search with **corrupted / broken locators** (e.g. `id='obsolete-broken-keyword-input-9999'`).
3. Intercept the failure, heal the elements in real-time, and submit the search.
4. Extract 5 live job listings and print them in a formatted table.
5. Generate an interactive HTML report at `target/healing-reports/healing-report.html`.

### Option 2: Run via TestNG Test Suite
Run the automated test suite containing both standard search and self-healing verification:

```bash
mvn clean test
```

---

## 📊 Self-Healing Audit Report (`healing-report.html`)

Upon completion, the framework writes a self-contained HTML dashboard to:
`target/healing-reports/healing-report.html`

The report includes:
- **Total Healing Events** count and **Self-Healing Success Rate %**.
- **Interactive Audit Table**:
  - Broken Original Selector (strikethrough) vs. Healed Working Selector.
  - Strategy used (`FALLBACK_SELECTOR`, `SIMILARITY_MATCHING`, or `STRUCTURAL_ANCHOR`).
  - Confidence percentage bar.
  - Direct link to the captured screenshot showing the element highlighted in the browser.

---

## 💡 How to Add Self-Healing to Your Own Pages

1. **Add an entry in `locators-repo.json`**:
```json
"my_app.submit_btn": {
  "key": "my_app.submit_btn",
  "primaryLocatorType": "css",
  "primaryLocatorValue": "button.submit-order-v1",
  "fallbackLocators": [
    "//button[contains(., 'Place Order')]",
    "button[type='submit']"
  ],
  "fingerprint": {
    "tag": "button",
    "type": "submit",
    "text": "Place Order",
    "aria-label": "Submit Order"
  }
}
```

2. **Use `HealingDriver` in your Page Object**:
```java
// Automatic healing when primary selector breaks:
driver.findElement("my_app.submit_btn", By.cssSelector("button.submit-order-v1")).click();
```
