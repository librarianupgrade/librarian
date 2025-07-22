package com.strange.common.utils;

import java.util.*;

/**
 * Utility class for various string similarity metrics in Java.
 */
public class StringSimilarityUtil {

    /**
     * Computes a ratio based on the length of the Longest Common Subsequence (LCS).
     * Approximation of difflib.SequenceMatcher.ratio().
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity ratio between 0 and 1
     */
    public static double sequenceRatio(String s1, String s2) {
        int lcsLen = lcsLength(s1, s2);
        return (2.0 * lcsLen) / (s1.length() + s2.length());
    }

    // Helper method to compute LCS length via dynamic programming
    private static int lcsLength(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    /**
     * Computes the Jaro–Winkler similarity between two strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Jaro–Winkler similarity (0.0 to 1.0)
     */
    public static double jaroWinklerSimilarity(String s1, String s2) {
        double jaroDist = jaroDistance(s1, s2);
        int prefixLen = commonPrefixLength(s1, s2);
        double scalingFactor = 0.1;
        return jaroDist + prefixLen * scalingFactor * (1 - jaroDist);
    }

    // Jaro distance calculation
    private static double jaroDistance(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length(), len2 = s2.length();
        int matchDistance = Math.max(len1, len2) / 2 - 1;

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (!s2Matches[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matches[i] = s2Matches[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (s1Matches[i]) {
                while (!s2Matches[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions += 0.5;
                k++;
            }
        }
        return ((matches / (double) len1) + (matches / (double) len2) + ((matches - transpositions) / matches)) / 3.0;
    }

    // Compute length of common prefix up to a maximum of 4
    private static int commonPrefixLength(String s1, String s2) {
        int n = Math.min(Math.min(s1.length(), s2.length()), 4);
        for (int i = 0; i < n; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return n;
    }

    /**
     * Computes Jaccard similarity between two strings using character n-grams.
     *
     * @param s1 First string
     * @param s2 Second string
     * @param n  Length of n-gram (e.g., 2 for bigrams)
     * @return Jaccard similarity (0.0 to 1.0)
     */
    public static double jaccard(String s1, String s2, int n) {
        Set<String> set1 = ngrams(s1, n);
        Set<String> set2 = ngrams(s2, n);
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // Helper to generate n-gram set
    private static Set<String> ngrams(String s, int n) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < s.length() - n + 1; i++) {
            result.add(s.substring(i, i + n));
        }
        return result;
    }

    /**
     * Computes similarity based on Levenshtein edit distance.
     * Similarity = 1 - (distance / maxLength)
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity ratio between 0 and 1
     */
    public static double levenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) distance / maxLen;
    }

    // Classic DP-based Levenshtein distance
    private static int levenshteinDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[m][n];
    }
}
