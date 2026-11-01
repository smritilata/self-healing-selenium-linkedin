package com.automation.healing.util;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.*;

/**
 * Utility class to calculate multi-attribute similarity between UI elements.
 * Uses Jaro-Winkler distance and token set overlap to resist dynamic class names and attribute changes.
 */
public final class SimilarityUtils {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    private SimilarityUtils() {}

    /**
     * Calculates Jaro-Winkler string similarity between two strings (0.0 to 1.0).
     */
    public static double stringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        String clean1 = s1.trim().toLowerCase(Locale.ROOT);
        String clean2 = s2.trim().toLowerCase(Locale.ROOT);
        if (clean1.isEmpty() && clean2.isEmpty()) {
            return 1.0;
        }
        if (clean1.isEmpty() || clean2.isEmpty()) {
            return 0.0;
        }
        if (clean1.equals(clean2)) {
            return 1.0;
        }
        return JARO_WINKLER.apply(clean1, clean2);
    }

    /**
     * Calculates Jaccard similarity between two sets of class tokens or words.
     * Helpful when CSS classes have both static semantic tokens and dynamic hash tokens.
     */
    public static double tokenSetSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        Set<String> set1 = new HashSet<>(Arrays.asList(str1.trim().toLowerCase(Locale.ROOT).split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(str2.trim().toLowerCase(Locale.ROOT).split("\\s+")));

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Computes a weighted overall similarity score between target fingerprint and a candidate's actual attributes.
     */
    public static double computeElementScore(Map<String, String> expected, Map<String, String> candidate) {
        if (expected == null || candidate == null || expected.isEmpty()) {
            return 0.0;
        }

        double totalWeight = 0.0;
        double accumulatedScore = 0.0;

        // Weights assigned to key element features
        Map<String, Double> weights = Map.of(
                "tag", 1.5,
                "aria-label", 2.0,
                "placeholder", 2.0,
                "name", 1.5,
                "text", 1.8,
                "type", 1.0,
                "role", 1.2,
                "id", 1.0,
                "class", 0.8
        );

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            String expValue = entry.getValue();
            if (expValue == null || expValue.isBlank()) {
                continue;
            }

            double weight = weights.getOrDefault(key, 0.5);
            totalWeight += weight;

            String actValue = candidate.get(key);
            if (actValue == null || actValue.isBlank()) {
                continue;
            }

            double sim;
            if ("tag".equalsIgnoreCase(key) || "type".equalsIgnoreCase(key)) {
                sim = expValue.equalsIgnoreCase(actValue) ? 1.0 : 0.0;
            } else if ("class".equalsIgnoreCase(key)) {
                sim = tokenSetSimilarity(expValue, actValue);
            } else {
                sim = stringSimilarity(expValue, actValue);
            }

            accumulatedScore += (sim * weight);
        }

        return totalWeight > 0.0 ? (accumulatedScore / totalWeight) : 0.0;
    }
}
