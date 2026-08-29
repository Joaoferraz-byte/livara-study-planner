package com.joaoferraz.livara.studyplanner.domain;

import java.util.Arrays;
import java.util.Locale;

/**
 * Reusable study-workflow intentions. Future templates can be added here;
 * the current product intentionally ships with one requested template.
 */
public enum WorkflowTemplate {
    MARKET_PROGRAMMING(
            "market-programming",
            "Market programming",
            "Professional practice as the main axis, supported by applied projects and spaced review.",
            FocusArea.MARKET_PROGRAMMING,
            FocusArea.PROJECTS);

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
