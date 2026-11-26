package com.hivemind.core;

import java.nio.file.Files;

public final class HiveSettingsCheck {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("hivemind-settings");
        var file = directory.resolve("hivemind.properties");
        try {
            if (!HiveSettings.load(file).equals(HiveSettings.DEFAULT) || !Files.isRegularFile(file))
                throw new AssertionError("missing default settings");
            Files.writeString(file, "naturalColoniesPerDimension=0\nhunting=false\nupkeepPerFourMembers=2\n");
            var settings = HiveSettings.load(file);
            if (settings.naturalColoniesPerDimension() != 0 || settings.hunting() || settings.upkeep(0) != 0
                    || settings.upkeep(4) != 2 || settings.upkeep(5) != 4 || !settings.expansion())
                throw new AssertionError("partial settings or population upkeep are incorrect");
            Files.writeString(file, "colonySpacing=0\n");
            try {
                HiveSettings.load(file);
                throw new AssertionError("invalid spacing was accepted");
            } catch (IllegalArgumentException expected) {
                if (!Files.readString(file).equals("colonySpacing=0\n"))
                    throw new AssertionError("invalid file was overwritten");
            }
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
        System.out.println("World settings defaults, overrides and upkeep passed.");
    }
}
