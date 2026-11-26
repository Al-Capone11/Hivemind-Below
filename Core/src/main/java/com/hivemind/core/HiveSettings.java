package com.hivemind.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record HiveSettings(int naturalColoniesPerDimension, int colonySpacing, boolean hunting,
                           int upkeepPerFourMembers, boolean expansion) {
    public static final HiveSettings DEFAULT = new HiveSettings(4, 128, true, 1, true);

    public static HiveSettings load(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, """
                    # 0 disables natural colonies. Existing colonies are unaffected.
                    naturalColoniesPerDimension=4
                    colonySpacing=128
                    hunting=true
                    # Food units per four members per Minecraft day. 0 disables upkeep.
                    upkeepPerFourMembers=1
                    expansion=true
                    """, java.nio.file.StandardOpenOption.CREATE_NEW);
        }

        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            values.load(reader);
        }
        return new HiveSettings(
                number(values, "naturalColoniesPerDimension", DEFAULT.naturalColoniesPerDimension(), 0, 64),
                number(values, "colonySpacing", DEFAULT.colonySpacing(), 32, 1024),
                flag(values, "hunting", DEFAULT.hunting()),
                number(values, "upkeepPerFourMembers", DEFAULT.upkeepPerFourMembers(), 0, 16),
                flag(values, "expansion", DEFAULT.expansion()));
    }

    private static int number(Properties values, String key, int fallback, int min, int max) {
        int result = Integer.parseInt(values.getProperty(key, Integer.toString(fallback)).trim());
        if (result < min || result > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
        return result;
    }

    private static boolean flag(Properties values, String key, boolean fallback) {
        return switch (values.getProperty(key, Boolean.toString(fallback)).trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(key + " must be true or false");
        };
    }

    public int upkeep(int population) {
        return (int) ((Math.max(0, population) + 3L) / 4 * upkeepPerFourMembers);
    }
}
