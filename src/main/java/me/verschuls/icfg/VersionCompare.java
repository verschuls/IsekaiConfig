package me.verschuls.icfg;

import java.util.Arrays;
import java.util.regex.Pattern;

class VersionCompare {
    
    public enum Result {
        GREATER,
        EQUAL,
        LESSER
    }

    public static Pattern PATTERN = Pattern.compile("^\\d+(\\.\\d+)*$");
    
    public static Result compare(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        
        String[] parts1 = normalizeAndSplit(version1);
        String[] parts2 = normalizeAndSplit(version2);
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (num1 > num2) {
                return Result.GREATER;
            } else if (num1 < num2) {
                return Result.LESSER;
            }
        }
        
        return Result.EQUAL;
    }
    
    public static boolean isValid(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = version.trim();
        if (!trimmed.matches(PATTERN.pattern())) {
            return false;
        }
        
        return Arrays.stream(trimmed.split("\\."))
                .allMatch(part -> !part.isEmpty() && part.matches("\\d+"));
    }
    
    private static String[] normalizeAndSplit(String version) {
        String trimmed = version.trim();
        if (!trimmed.matches(PATTERN.pattern())) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
        return trimmed.split("\\.");
    }
    
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number: " + part);
        }
    }
}
