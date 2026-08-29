package com.joaoferraz.livara.studyplanner.domain;

import java.util.Arrays;
import java.util.Locale;

public enum FocusArea {
    MARKET_PROGRAMMING("market-programming", "Market programming", 5),
    PROJECTS("projects", "Applied projects", 5),
    LOGIC("logic", "Logic and implementation", 4),
    ARCHITECTURE("architecture", "Software architecture", 3),
    OPTIMIZATION("optimization", "Software optimization", 3),
    PHYSICS("physics", "Physics", 2),
    BIOLOGY("biology", "Biology", 2),
    CHEMISTRY("chemistry", "Chemistry", 2),
    MATHEMATICS("mathematics", "Mathematics", 2),
    REVIEW("review", "Review and planning", 3);

    private final String id;
    private final String label;
    private final int priority;

    FocusArea(String id, String label, int priority) {
        this.id = id;
        this.label = label;
        this.priority = priority;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int priority() {
        return priority;
    }

    public static FocusArea fromId(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(area -> area.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown focus area: " + value));
    }
}
