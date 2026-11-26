package com.hivemind.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ColonyStatus {
    private ColonyStatus() {}

    public static String summary(Collection<Colony> colonies) {
        int active = 0, members = 0;
        for (Colony colony : colonies) {
            if (colony.state() != ColonyState.ACTIVE) continue;
            active++;
            members += colony.totalPopulation();
        }
        return "Hivemind · Colonias activas: " + active + " · Integrantes: " + members;
    }

    public static List<String> lines(Colony colony, String capacity, String food, String hunting, List<String> outposts) {
        List<String> lines = new ArrayList<>();
        lines.add("Colmena " + colony.id());
        var home = colony.coreLocation();
        String position = home == null ? "" : " · Núcleo: " + coordinates(home) + " · " + home.dimension();
        lines.add("  " + state(colony.state()) + " · " + stage(colony.developmentStage()) + position);
        lines.add("  Población: " + colony.totalPopulation() + "/" + capacity + " · " + colony.population(Caste.WORKER)
                + " obreros · Cría: " + caste(colony.broodCaste()));
        lines.add("  Alimento: " + food + " · Caza: " + hunting);
        lines.add("  Cámaras: " + (colony.hasOrganicNest() ? (colony.hasSpecializedRooms() ? "guardia, almacén, descanso y cámara real con cría" : "refugio con cámara real y cría") : colony.hasSpecializedRooms() ? "5" : "tradicional")
                + " · Territorio: " + colony.territoryRadius() + " bloques");
        lines.add("  Asentamientos (" + colony.outposts().size() + ")");
        for (String outpost : outposts) lines.add("    " + outpost);
        var memories = colony.memorySnapshot();
        if (!memories.isEmpty()) {
            lines.add("  Memoria de jugadores");
            memories.values().stream()
                    .sorted(Comparator.comparing(PlayerMemory::threatLevel).reversed().thenComparing(PlayerMemory::playerId))
                    .forEach(memory -> {
                        lines.add("    " + memory.playerId() + " · " + threat(memory.threatLevel()));
                        lines.add("      Encuentros: " + memory.encounters() + " · Bajas: " + memory.colonyLosses()
                                + " · Daños: " + memory.structuralDamage());
                        if (!memory.tactics().isEmpty()) lines.add("      Tácticas: " + memory.tactics().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey()).map(entry -> tactic(entry.getKey()) + " " + entry.getValue())
                                .collect(java.util.stream.Collectors.joining(" · ")));
                    });
        }
        return List.copyOf(lines);
    }

    public static String coordinates(ColonyLocation location) {
        return location.x() + ", " + location.y() + ", " + location.z();
    }

    private static String state(ColonyState state) {
        return switch (state) {
            case ACTIVE -> "Activa";
            case COLLAPSING -> "En colapso";
            case EXTERMINATED -> "Exterminada";
        };
    }

    private static String stage(String stage) {
        return switch (stage) {
            case "NASCENT" -> "incipiente";
            case "ESTABLISHED" -> "establecida";
            case "DEVELOPED" -> "desarrollada";
            case "DOMINANT" -> "dominante";
            default -> stage;
        };
    }

    private static String caste(Caste caste) {
        if (caste == null) return "ninguna";
        return switch (caste) {
            case QUEEN -> "reina";
            case WORKER -> "obrero";
            case SCOUT -> "explorador";
            case WARRIOR -> "combatiente";
        };
    }

    private static String threat(ThreatLevel threat) {
        return switch (threat) {
            case UNKNOWN -> "Desconocido";
            case DETECTED -> "Detectado";
            case THREAT -> "Amenaza";
            case HOSTILE -> "Hostil";
            case CRITICAL -> "Crítico";
        };
    }

    private static String tactic(CombatTactic tactic) {
        return switch (tactic) {
            case MELEE -> "cuerpo a cuerpo";
            case RANGED -> "a distancia";
            case FIRE -> "fuego";
            case STRUCTURE -> "daño a estructuras";
            case INCURSION -> "incursión";
        };
    }
}
