package com.joaoferraz.livara.studyplanner.domain;

import java.util.Arrays;
import java.util.Locale;

public enum FocusArea {
    MARKET_PROGRAMMING("market-programming", "Programação para o mercado", 5),
    PROJECTS("projects", "Projetos aplicando o estudo", 5),
    LOGIC("logic", "Lógica e implementação", 4),
    ARCHITECTURE("architecture", "Arquitetura de software", 3),
    OPTIMIZATION("optimization", "Otimização de software", 3),
    PHYSICS("physics", "Física", 2),
    BIOLOGY("biology", "Biologia", 2),
    CHEMISTRY("chemistry", "Química", 2),
    MATHEMATICS("mathematics", "Matemática", 2),
    REVIEW("review", "Revisão e planejamento", 3);

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
