package com.joaoferraz.livara.studyplanner.domain;

import java.util.Arrays;
import java.util.Locale;

/**
 * Reusable study-workflow intentions. A template is not a weekday schedule:
 * it describes what deserves more repetitions when the weekly sequence is
 * generated.
 */
public enum WorkflowTemplate {
    MARKET_PROGRAMMING(
            "market-programming",
            "Programação para o mercado",
            "Prática profissional como eixo principal, com projetos aplicados e revisão espaçada.",
            FocusArea.MARKET_PROGRAMMING,
            FocusArea.PROJECTS),
    APPLIED_PROJECTS(
            "applied-projects",
            "Projetos aplicando o estudo",
            "Construção de projetos como eixo principal, apoiada por programação profissional.",
            FocusArea.PROJECTS,
            FocusArea.MARKET_PROGRAMMING),
    LOGIC_IMPLEMENTATION(
            "logic-implementation",
            "Treino de lógica e implementação",
            "Problemas e implementação intercalados com aplicação em projeto.",
            FocusArea.LOGIC,
            FocusArea.PROJECTS),
    SOFTWARE_ARCHITECTURE(
            "software-architecture",
            "Arquitetura de software",
            "Decisões de arquitetura conectadas a código real e revisão de fundamentos.",
            FocusArea.ARCHITECTURE,
            FocusArea.PROJECTS),
    SOFTWARE_OPTIMIZATION(
            "software-optimization",
            "Otimização de software",
            "Medição, diagnóstico e otimização aplicados a projetos reais.",
            FocusArea.OPTIMIZATION,
            FocusArea.PROJECTS),
    SCHOOL_SUBJECTS(
            "school-subjects",
            "Matérias escolares",
            "Alternância entre matérias escolares, com otimização como foco complementar.",
            FocusArea.OPTIMIZATION,
            FocusArea.MARKET_PROGRAMMING);

    private final String id;
    private final String label;
    private final String description;
    private final FocusArea primaryFocus;
    private final FocusArea secondaryFocus;

    WorkflowTemplate(String id, String label, String description, FocusArea primaryFocus, FocusArea secondaryFocus) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.primaryFocus = primaryFocus;
        this.secondaryFocus = secondaryFocus;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public FocusArea primaryFocus() {
        return primaryFocus;
    }

    public FocusArea secondaryFocus() {
        return secondaryFocus;
    }

    public static WorkflowTemplate fromId(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(template -> template.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow template: " + value));
    }
}
