package com.joaoferraz.livara.studyplanner.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A named rotation in a reusable schedule.
 *
 * The original application used an enum with exactly A and B. The constants
 * remain as compatibility presets, while user-created cycles carry their own
 * stable id, label, description and optional focus requirements.
 */
public final class Cycle {
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    public static final Cycle A = preset("A", "A", "Physics + Biology",
            List.of(FocusArea.PHYSICS, FocusArea.BIOLOGY));
    public static final Cycle B = preset("B", "B", "Chemistry + Mathematics",
            List.of(FocusArea.CHEMISTRY, FocusArea.MATHEMATICS));

    private final String id;
    private final String label;
    private final String subjects;
    private final List<FocusArea> requiredFocuses;

    private Cycle(String id, String label, String subjects, List<FocusArea> requiredFocuses) {
        this.id = normalizeId(id);
        this.label = requireText(label, "label");
        this.subjects = requireText(subjects, "subjects");
        Objects.requireNonNull(requiredFocuses, "requiredFocuses");
        this.requiredFocuses = List.copyOf(new ArrayList<>(requiredFocuses));
    }

    private static Cycle preset(String id, String label, String subjects, List<FocusArea> requiredFocuses) {
        return new Cycle(id.toLowerCase(Locale.ROOT), label, subjects, requiredFocuses);
    }

    public static Cycle custom(String id, String label, String subjects, List<FocusArea> requiredFocuses) {
        return new Cycle(id, label, subjects, requiredFocuses);
    }

    /** Parses a persisted id and retains the two legacy preset semantics. */
    public static Cycle fromId(String value) {
        Objects.requireNonNull(value, "cycle id");
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("A")) {
            return A;
        }
        if (normalized.equalsIgnoreCase("B")) {
            return B;
        }
        String id = normalizeId(normalized);
        return new Cycle(id, normalized, "Custom cycle", List.of());
    }

    public static Cycle fromDefinition(String id, String label, String subjects,
                                       List<FocusArea> requiredFocuses) {
        return new Cycle(id, label, subjects, requiredFocuses);
    }

    /** Compatibility with callers that previously used enum constants. */
    public static Cycle valueOf(String value) {
        return fromId(value);
    }

    /** Compatibility list of built-in presets; custom cycles are template-local. */
    public static Cycle[] values() {
        return new Cycle[] { A, B };
    }

    public String id() {
        return id;
    }

    /** Compatibility with enum-backed JSON and callers. */
    public String name() {
        return id.equals("a") ? "A" : id.equals("b") ? "B" : id;
    }

    public String label() {
        return label;
    }

    public String subjects() {
        return subjects;
    }

    public List<FocusArea> requiredFocuses() {
        return requiredFocuses;
    }

    /** Compatibility behavior for the original two preset constants. */
    public Cycle next() {
        if (equals(A)) {
            return B;
        }
        if (equals(B)) {
            return A;
        }
        return this;
    }

    private static String normalizeId(String value) {
        String normalized = requireText(value, "id").toLowerCase(Locale.ROOT);
        if (!ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Cycle id must use lowercase letters, numbers, '-' or '_' and be at most 64 characters");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Cycle " + field + " cannot be blank");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Cycle cycle && id.equals(cycle.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }
}
