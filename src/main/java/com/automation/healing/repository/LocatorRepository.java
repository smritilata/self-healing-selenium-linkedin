package com.automation.healing.repository;

import com.automation.healing.model.LocatorProfile;
import com.automation.healing.util.ConfigLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe persistent repository managing locator profiles and healing history.
 */
public class LocatorRepository {

    private static final Logger logger = LoggerFactory.getLogger(LocatorRepository.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static LocatorRepository instance;

    private final Map<String, LocatorProfile> profiles = new ConcurrentHashMap<>();
    private final String repoPath;
    private final boolean autoUpdate;

    private LocatorRepository() {
        this.repoPath = ConfigLoader.get("healing.repo.path", "src/main/resources/locators-repo.json");
        this.autoUpdate = ConfigLoader.getBoolean("healing.auto.update.repo", true);
        loadProfiles();
    }

    public static synchronized LocatorRepository getInstance() {
        if (instance == null) {
            instance = new LocatorRepository();
        }
        return instance;
    }

    private void loadProfiles() {
        try {
            File file = new File(repoPath);
            if (file.exists()) {
                Map<String, LocatorProfile> data = MAPPER.readValue(file, new TypeReference<Map<String, LocatorProfile>>() {});
                profiles.putAll(data);
                logger.info("Loaded {} locator profiles from file: {}", profiles.size(), file.getAbsolutePath());
                return;
            }

            // Fallback to classpath if running inside jar/packaged environment
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("locators-repo.json")) {
                if (is != null) {
                    Map<String, LocatorProfile> data = MAPPER.readValue(is, new TypeReference<Map<String, LocatorProfile>>() {});
                    profiles.putAll(data);
                    logger.info("Loaded {} locator profiles from classpath", profiles.size());
                } else {
                    logger.warn("No locators-repo.json found at path or classpath. Initializing empty repository.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load locator repository from {}", repoPath, e);
        }
    }

    public LocatorProfile getProfile(String key) {
        return profiles.get(key);
    }

    public Map<String, LocatorProfile> getAllProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public synchronized void registerProfile(LocatorProfile profile) {
        if (profile == null || profile.getKey() == null) return;
        profiles.put(profile.getKey(), profile);
        persistIfEnabled();
    }

    public synchronized void recordHealingSuccess(String key, String healedLocator, double score) {
        LocatorProfile profile = profiles.get(key);
        if (profile == null) {
            profile = new LocatorProfile(key, "xpath", healedLocator);
            profiles.put(key, profile);
        }

        profile.incrementHealCount();
        profile.setLastKnownWorkingLocator(healedLocator);
        profile.setLastHealedTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        profile.addFallbackLocator(healedLocator);

        logger.info("Updated repository for key '{}' (Total heals: {}) with healed selector: {}",
                key, profile.getHealCount(), healedLocator);

        persistIfEnabled();
    }

    private synchronized void persistIfEnabled() {
        if (!autoUpdate) return;
        try {
            File file = new File(repoPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            MAPPER.writeValue(file, profiles);
            logger.debug("Successfully persisted updated locator repository to {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Could not persist locator repository changes to disk: {}", e.getMessage());
        }
    }
}
