package com.automation.healing.report;

import com.automation.healing.model.HealingResult;
import com.automation.healing.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages healing audit trails and generates HTML dashboard reports.
 */
public class HealingReportManager {

    private static final Logger logger = LoggerFactory.getLogger(HealingReportManager.class);
    private static HealingReportManager instance;

    private final List<HealingResult> results = Collections.synchronizedList(new ArrayList<>());
    private final String reportOutputDir;

    private HealingReportManager() {
        this.reportOutputDir = ConfigLoader.get("healing.report.output.dir", "target/healing-reports");
    }

    public static synchronized HealingReportManager getInstance() {
        if (instance == null) {
            instance = new HealingReportManager();
        }
        return instance;
    }

    public void recordHealingResult(HealingResult result) {
        if (result != null) {
            results.add(result);
            logger.info("Recorded healing event: Key='{}', Status={}, Strategy='{}', Score={}",
                    result.getElementKey(),
                    result.isSuccessful() ? "SUCCESS" : "FAILED",
                    result.getStrategy(),
                    String.format("%.1f%%", result.getConfidenceScore() * 100));
        }
    }

    public List<HealingResult> getResults() {
        return Collections.unmodifiableList(new ArrayList<>(results));
    }

    public void clear() {
        results.clear();
    }

    /**
     * Generates a modern HTML self-healing report dashboard.
     */
    public synchronized String generateHtmlReport() {
        try {
            Path outputDir = Paths.get(reportOutputDir);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            File reportFile = outputDir.resolve("healing-report.html").toFile();

            long total = results.size();
            long successful = results.stream().filter(HealingResult::isSuccessful).count();
            long failed = total - successful;
            double successRate = total > 0 ? ((double) successful / total) * 100 : 100.0;

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("  <meta charset=\"UTF-8\" />\n")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n")
                .append("  <title>Autonomous Self-Healing Test Automation Report</title>\n")
                .append("  <style>\n")
                .append("    :root { --bg: #0f172a; --card: #1e293b; --text: #f8fafc; --accent: #38bdf8; --success: #10b981; --warn: #f59e0b; --danger: #ef4444; --border: #334155; }\n")
                .append("    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n")
                .append("    body { background-color: var(--bg); color: var(--text); padding: 32px; }\n")
                .append("    .container { max-width: 1200px; margin: 0 auto; }\n")
                .append("    header { margin-bottom: 32px; border-bottom: 1px solid var(--border); padding-bottom: 20px; }\n")
                .append("    header h1 { font-size: 28px; font-weight: 700; color: #fff; display: flex; align-items: center; gap: 10px; }\n")
                .append("    header p { color: #94a3b8; font-size: 14px; margin-top: 6px; }\n")
                .append("    .badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: uppercase; }\n")
                .append("    .badge-success { background: rgba(16, 185, 129, 0.2); color: var(--success); border: 1px solid var(--success); }\n")
                .append("    .badge-danger { background: rgba(239, 68, 68, 0.2); color: var(--danger); border: 1px solid var(--danger); }\n")
                .append("    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 36px; }\n")
                .append("    .metric-card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }\n")
                .append("    .metric-title { font-size: 13px; text-transform: uppercase; letter-spacing: 0.05em; color: #94a3b8; margin-bottom: 8px; }\n")
                .append("    .metric-val { font-size: 32px; font-weight: 700; }\n")
                .append("    .val-accent { color: var(--accent); }\n")
                .append("    .val-success { color: var(--success); }\n")
                .append("    .val-danger { color: var(--danger); }\n")
                .append("    .panel { background: var(--card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }\n")
                .append("    .panel-header { padding: 18px 24px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }\n")
                .append("    .panel-header h2 { font-size: 18px; }\n")
                .append("    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }\n")
                .append("    th { background: #182234; padding: 14px 20px; color: #94a3b8; font-weight: 600; text-transform: uppercase; font-size: 12px; }\n")
                .append("    td { padding: 16px 20px; border-top: 1px solid var(--border); vertical-align: top; }\n")
                .append("    tr:hover { background: rgba(255, 255, 255, 0.02); }\n")
                .append("    code { background: #0b1120; padding: 3px 6px; border-radius: 4px; font-family: monospace; font-size: 12px; color: #38bdf8; word-break: break-all; }\n")
                .append("    .strike { text-decoration: line-through; color: #ef4444; opacity: 0.85; }\n")
                .append("    .healed-loc { color: #10b981; font-weight: 600; display: block; margin-top: 4px; }\n")
                .append("    .progress-bar-container { background: #0b1120; border-radius: 6px; height: 10px; width: 100px; overflow: hidden; display: inline-block; vertical-align: middle; margin-right: 8px; }\n")
                .append("    .progress-bar { background: var(--success); height: 100%; }\n")
                .append("    .img-link { color: var(--accent); text-decoration: none; font-size: 12px; font-weight: 600; }\n")
                .append("    .img-link:hover { text-decoration: underline; }\n")
                .append("  </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("  <div class=\"container\">\n")
                .append("    <header>\n")
                .append("      <h1><span>🛡️</span> Self-Healing Selenium Test Report</h1>\n")
                .append("      <p>Target: LinkedIn Job Search Automation | Autonomous DOM Healing & Recovery</p>\n")
                .append("    </header>\n\n")
                .append("    <div class=\"metrics-grid\">\n")
                .append("      <div class=\"metric-card\"><div class=\"metric-title\">Total Healing Events</div><div class=\"metric-val val-accent\">").append(total).append("</div></div>\n")
                .append("      <div class=\"metric-card\"><div class=\"metric-title\">Successfully Healed</div><div class=\"metric-val val-success\">").append(successful).append("</div></div>\n")
                .append("      <div class=\"metric-card\"><div class=\"metric-title\">Failed Heals</div><div class=\"metric-val val-danger\">").append(failed).append("</div></div>\n")
                .append("      <div class=\"metric-card\"><div class=\"metric-title\">Self-Healing Rate</div><div class=\"metric-val val-success\">").append(String.format("%.1f%%", successRate)).append("</div></div>\n")
                .append("    </div>\n\n")
                .append("    <div class=\"panel\">\n")
                .append("      <div class=\"panel-header\">\n")
                .append("        <h2>Healing Audit Trail</h2>\n")
                .append("        <span class=\"badge badge-success\">").append(successful).append(" Resolved</span>\n")
                .append("      </div>\n")
                .append("      <table>\n")
                .append("        <thead>\n")
                .append("          <tr>\n")
                .append("            <th>Timestamp</th>\n")
                .append("            <th>Element Key</th>\n")
                .append("            <th>Status</th>\n")
                .append("            <th>Original Broken Locator &rarr; Healed Locator</th>\n")
                .append("            <th>Strategy</th>\n")
                .append("            <th>Confidence</th>\n")
                .append("            <th>Evidence</th>\n")
                .append("          </tr>\n")
                .append("        </thead>\n")
                .append("        <tbody>\n");

            if (results.isEmpty()) {
                html.append("          <tr><td colspan=\"7\" style=\"text-align:center; color:#94a3b8; padding:32px;\">No healing events triggered during this run (all primary locators were intact or no broken selectors encountered).</td></tr>\n");
            } else {
                for (HealingResult res : results) {
                    int percent = (int) Math.round(res.getConfidenceScore() * 100);
                    html.append("          <tr>\n")
                        .append("            <td style=\"color:#94a3b8; font-size:12px;\">").append(res.getTimestamp()).append("</td>\n")
                        .append("            <td><strong>").append(res.getElementKey()).append("</strong></td>\n")
                        .append("            <td>")
                        .append(res.isSuccessful()
                                ? "<span class=\"badge badge-success\">HEALED</span>"
                                : "<span class=\"badge badge-danger\">FAILED</span>")
                        .append("</td>\n")
                        .append("            <td>\n")
                        .append("              <div class=\"strike\"><code>").append(escapeHtml(res.getOriginalLocator())).append("</code></div>\n")
                        .append("              <div class=\"healed-loc\"><code>&rarr; ").append(escapeHtml(res.getHealedLocator())).append("</code></div>\n")
                        .append("            </td>\n")
                        .append("            <td><span style=\"color:#cbd5e1;\">").append(res.getStrategy()).append("</span></td>\n")
                        .append("            <td>\n")
                        .append("              <div class=\"progress-bar-container\"><div class=\"progress-bar\" style=\"width: ").append(percent).append("%;\"></div></div>\n")
                        .append("              <span>").append(percent).append("%</span>\n")
                        .append("            </td>\n")
                        .append("            <td>\n");

                    if (res.getScreenshotPath() != null && !res.getScreenshotPath().isBlank()) {
                        File imgFile = new File(res.getScreenshotPath());
                        html.append("              <a class=\"img-link\" href=\"screenshots/").append(imgFile.getName()).append("\" target=\"_blank\">View Screenshot &nearr;</a>\n");
                    } else {
                        html.append("              <span style=\"color:#64748b;\">N/A</span>\n");
                    }

                    html.append("            </td>\n")
                        .append("          </tr>\n");
                }
            }

            html.append("        </tbody>\n")
                .append("      </table>\n")
                .append("    </div>\n")
                .append("  </div>\n")
                .append("</body>\n")
                .append("</html>\n");

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(html.toString());
            }

            String absolutePath = reportFile.getAbsolutePath();
            logger.info("=================================================================");
            logger.info("HTML Self-Healing Report generated at: {}", absolutePath);
            logger.info("=================================================================");
            return absolutePath;

        } catch (Exception e) {
            logger.error("Failed to generate HTML healing report", e);
            return null;
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
