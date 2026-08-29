package com.joaoferraz.livara.studyplanner.ui;

import com.joaoferraz.livara.studyplanner.domain.FocusArea;

import java.util.Objects;

record StudySessionItem(
        String id,
        boolean pause,
        int order,
        FocusArea focus,
        String title,
        String subtitle,
        int durationMinutes,
        String glyph) {

    StudySessionItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        Objects.requireNonNull(glyph, "glyph");
        if (!pause) {
            Objects.requireNonNull(focus, "focus");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }

    static StudySessionItem study(int order, FocusArea focus, String title, int minutes) {
        return new StudySessionItem("study-" + order, false, order, focus, title,
                focus.label(), minutes, glyphFor(focus));
    }

    static StudySessionItem breakItem(int order, int minutes) {
        return new StudySessionItem("pause-" + order, true, order, null, "Recovery pause",
                "Step away, hydrate, and return with intention", minutes, "◷");
    }

    private static String glyphFor(FocusArea focus) {
        return switch (focus) {
            case MARKET_PROGRAMMING -> "⌘";
            case PROJECTS -> "◆";
            case LOGIC -> "∴";
            case ARCHITECTURE -> "◇";
            case OPTIMIZATION -> "↗";
            case PHYSICS -> "∆";
            case BIOLOGY -> "✣";
            case CHEMISTRY -> "⚗";
            case MATHEMATICS -> "∑";
            case REVIEW -> "↺";
        };
    }
}
